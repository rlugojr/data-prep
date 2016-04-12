// ============================================================================
//
// Copyright (C) 2006-2016 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// https://github.com/Talend/data-prep/blob/master/LICENSE
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================

package org.talend.dataprep.dataset.service;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.*;
import org.talend.daikon.exception.ExceptionContext;
import org.talend.dataprep.api.dataset.*;
import org.talend.dataprep.api.dataset.DataSetGovernance.Certification;
import org.talend.dataprep.api.dataset.location.SemanticDomain;
import org.talend.dataprep.api.folder.FolderEntry;
import org.talend.dataprep.api.service.info.VersionService;
import org.talend.dataprep.api.user.UserData;
import org.talend.dataprep.dataset.configuration.EncodingSupport;
import org.talend.dataprep.dataset.service.analysis.DataSetAnalyzer;
import org.talend.dataprep.dataset.service.analysis.asynchronous.AsynchronousDataSetAnalyzer;
import org.talend.dataprep.dataset.service.analysis.asynchronous.StatisticsAnalysis;
import org.talend.dataprep.dataset.service.analysis.synchronous.*;
import org.talend.dataprep.dataset.service.api.UpdateColumnParameters;
import org.talend.dataprep.dataset.service.locator.DataSetLocatorService;
import org.talend.dataprep.dataset.store.content.ContentStoreRouter;
import org.talend.dataprep.dataset.store.metadata.DataSetMetadataRepository;
import org.talend.dataprep.exception.TDPException;
import org.talend.dataprep.exception.error.CommonErrorCodes;
import org.talend.dataprep.exception.error.DataSetErrorCodes;
import org.talend.dataprep.exception.json.JsonErrorCodeDescription;
import org.talend.dataprep.folder.store.FolderRepository;
import org.talend.dataprep.http.HttpResponseContext;
import org.talend.dataprep.lock.DistributedLock;
import org.talend.dataprep.log.Markers;
import org.talend.dataprep.metrics.Timed;
import org.talend.dataprep.metrics.VolumeMetered;
import org.talend.dataprep.schema.DraftValidator;
import org.talend.dataprep.schema.FormatGuess;
import org.talend.dataprep.schema.SchemaParserResult;
import org.talend.dataprep.security.Security;
import org.talend.dataprep.user.store.UserDataRepository;

import javax.annotation.PostConstruct;
import javax.jms.Message;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.*;

@RestController
@Api(value = "datasets", basePath = "/datasets", description = "Operations on data sets")
public class DataSetService {

    /**
     * This class' logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DataSetService.class);

    /**
     * Date format to use.
     */
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM-dd-YYYY HH:mm"); // $NON-NLS-1

    static {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$
    }

    /**
     * DQ asynchronous analyzers.
     */
    @Autowired
    private AsynchronousDataSetAnalyzer[] asynchronousAnalyzers;

    /**
     * DQ synchronous analyzers.
     */
    @Autowired
    private List<SynchronousDataSetAnalyzer> synchronousAnalyzers;

    /**
     * Format analyzer needed to update the schema.
     */
    @Autowired
    private FormatAnalysis formatAnalyzer;

    /**
     * Quality analyzer needed to compute quality on dataset sample.
     */
    @Autowired
    private QualityAnalysis qualityAnalyzer;

    /**
     * Statistics analyzer needed to compute statistics on dataset sample.
     */
    @Autowired
    private StatisticsAnalysis statisticsAnalysis;

    /**
     * JMS template used to call asynchronous analysers.
     */
    @Autowired
    private JmsTemplate jmsTemplate;

    /**
     * Dataset metadata repository.
     */
    @Autowired
    private DataSetMetadataRepository dataSetMetadataRepository;

    /**
     * Dataset content store.
     */
    @Autowired
    private ContentStoreRouter contentStore;

    /**
     * User repository.
     */
    @Autowired
    private UserDataRepository userDataRepository;

    /**
     * Format guess factory.
     */
    @Autowired
    private FormatGuess.Factory formatGuessFactory;

    /**
     * Dataset locator (used for remote datasets).
     */
    @Autowired
    private DataSetLocatorService datasetLocator;

    /**
     * DataPrep abstraction to the underlying security (whether it's enabled or not).
     */
    @Autowired
    private Security security;

    /**
     * Folder repository.
     */
    @Autowired
    private FolderRepository folderRepository;

    /**
     * Encoding support service.
     */
    @Autowired
    private EncodingSupport encodings;

    /**
     * DataSet metadata builder.
     */
    @Autowired
    private DataSetMetadataBuilder metadataBuilder;

    @Autowired
    private VersionService versionService;

    @Autowired
    InventoryUtils inventoryUtils;

    /**
     * Sort the synchronous analyzers.
     */
    @PostConstruct
    public void initialize() {
        synchronousAnalyzers.sort((analyzer1, analyzer2) -> analyzer1.order() - analyzer2.order());
    }

    /**
     * Performs the analysis on the given dataset id.
     *
     * @param id              the dataset id.
     * @param analysersToSkip the list of analysers to skip.
     */
    @SafeVarargs
    private final void queueEvents(String id, Class<? extends DataSetAnalyzer>... analysersToSkip) {

        List<Class<? extends DataSetAnalyzer>> toSkip = Arrays.asList(analysersToSkip);

        // Calls all synchronous analysis first
        for (SynchronousDataSetAnalyzer synchronousDataSetAnalyzer : synchronousAnalyzers) {
            if (toSkip.contains(synchronousDataSetAnalyzer.getClass())) {
                continue;
            }
            LOG.info("Running {}", synchronousDataSetAnalyzer.getClass());
            synchronousDataSetAnalyzer.analyze(id);
            LOG.info("Done running {}", synchronousDataSetAnalyzer.getClass());
        }

        // Then use JMS queue for all optional analysis
        for (AsynchronousDataSetAnalyzer asynchronousDataSetAnalyzer : asynchronousAnalyzers) {
            if (toSkip.contains(asynchronousDataSetAnalyzer.getClass())) {
                continue;
            }
            jmsTemplate.send(asynchronousDataSetAnalyzer.destination(), session -> {
                Message message = session.createMessage();
                message.setStringProperty("dataset.id", id); // $NON-NLS-1
                return message;
            });
        }
    }

    @RequestMapping(value = "/datasets", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "List all data sets", notes = "Returns the list of data sets the current user is allowed to see. Creation date is a Epoch time value (in UTC time zone).")
    @Timed
    public Iterable<DataSetMetadata> list(
            @ApiParam(value = "Sort key (by name or date).") @RequestParam(defaultValue = "DATE", required = false) String sort,
            @ApiParam(value = "Order for sort key (desc or asc).") @RequestParam(defaultValue = "DESC", required = false) String order,
            @ApiParam(value = "Folder id to search datasets") @RequestParam(defaultValue = "", required = false) String folder) {

        Spliterator<DataSetMetadata> iterator;
        if (StringUtils.isNotEmpty(folder)) {
            // TODO dataset must be a constant somewhere!!
            Iterable<FolderEntry> entries = folderRepository.entries(folder, FolderEntry.ContentType.DATASET);
            final List<DataSetMetadata> metadatas = new ArrayList<>();
            entries.forEach(folderEntry ->
            {
                DataSetMetadata dataSetMetadata =
                        dataSetMetadataRepository.get(folderEntry.getContentId());
                if (dataSetMetadata != null) {
                    metadatas.add(dataSetMetadataRepository.get(folderEntry.getContentId()));
                } else {
                    folderRepository.removeFolderEntry(folderEntry.getFolderId(), //
                            folderEntry.getContentId(), //
                            folderEntry.getContentType());
                }
            });
            iterator = metadatas.spliterator();
        } else {
            iterator = dataSetMetadataRepository.list().spliterator();
        }

        Stream<DataSetMetadata> stream = StreamSupport.stream(iterator, false);

        final Comparator<String> comparisonOrder = getOrderComparator(order);
        final Comparator<DataSetMetadata> comparator = getDataSetMetadataComparator(sort, comparisonOrder);

        // Return sorted results
        return stream.filter(metadata -> !metadata.getLifecycle().importing()) //
                .map(metadata -> {
                    completeWithUserData(metadata);
                    return metadata;
                }) //
                .sorted(comparator) //
                .collect(Collectors.toList());
    }

    /**
     * Returns a list containing all data sets that are compatible with the data set with id <tt>dataSetId</tt>. If no
     * compatible data set is found an empty list is returned. The data set with id <tt>dataSetId</tt> is never returned
     * in the list.
     *
     * @param dataSetId the specified data set id
     * @param sort the sort criterion: either name or date.
     * @param order the sorting order: either asc or descgi
     * @return a list containing all data sets that are compatible with the data set with id <tt>dataSetId</tt> and
     * empty list if no data set is compatible.
     */
    @RequestMapping(value = "/datasets/{id}/compatibledatasets", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "List all compatible data sets", notes = "Returns the list of data sets the current user is allowed to see and which are compatible with the specified data set id.")
    @Timed
    public Iterable<DataSetMetadata> listCompatibleDatasets(
            @PathVariable(value = "id") @ApiParam(name = "id", value = "Id of the data set metadata") String dataSetId,
            @ApiParam(value = "Sort key (by name or date).") @RequestParam(defaultValue = "DATE", required = false) String sort,
            @ApiParam(value = "Order for sort key (desc or asc).") @RequestParam(defaultValue = "DESC", required = false) String order) {

        Spliterator<DataSetMetadata> iterator = dataSetMetadataRepository.listCompatible(dataSetId).spliterator();

        Stream<DataSetMetadata> stream = StreamSupport.stream(iterator, false);

        final Comparator<String> comparisonOrder = getOrderComparator(order);
        final Comparator<DataSetMetadata> comparator = getDataSetMetadataComparator(sort, comparisonOrder);

        // Return sorted results
        return stream.filter(metadata -> !metadata.getLifecycle().importing()) //
                .map(metadata -> {
                    completeWithUserData(metadata);
                    return metadata;
                }) //
                .sorted(comparator) //
                .collect(Collectors.toList());
    }

    /**
     * Creates a new data set and returns the new data set id as text in the response.
     *
     * @param name        An optional name for the new data set (might be <code>null</code>).
     * @param contentType the request content type.
     * @param content     The raw content of the data set (might be a CSV, XLS...) or the connection parameter in case of a
     *                    remote csv.
     * @return The new data id.
     * @see #get(boolean, Long, String)
     */
    @RequestMapping(value = "/datasets", method = POST, consumes = MediaType.ALL_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    @ApiOperation(value = "Create a data set", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE, notes = "Create a new data set based on content provided in POST body. For documentation purposes, body is typed as 'text/plain' but operation accepts binary content too. Returns the id of the newly created data set.")
    @Timed
    @VolumeMetered
    public String create(
            @ApiParam(value = "User readable name of the data set (e.g. 'Finance Report 2015', 'Test Data Set').") @RequestParam(defaultValue = "", required = false) String name,
            @RequestHeader("Content-Type") String contentType, @ApiParam(value = "content") InputStream content,
            @ApiParam(value = "The folder path to create the entry.") @RequestParam(defaultValue = "/", required = false) String folderPath)
            throws IOException {
        HttpResponseContext.header("Content-Type", MediaType.TEXT_PLAIN_VALUE);
        final String id = UUID.randomUUID().toString();
        final Marker marker = Markers.dataset(id);
        LOG.debug(marker, "Creating...");

        // get the location out of the content type and the request body
        DataSetLocation location;
        try {
            location = datasetLocator.getDataSetLocation(contentType, content);
        } catch (IOException e) {
            throw new TDPException(DataSetErrorCodes.UNABLE_TO_READ_DATASET_LOCATION, e);
        }

        DataSetMetadata dataSetMetadata = metadataBuilder.metadata() //
                .id(id) //
                .name(name) //
                .author(security.getUserId()) //
                .location(location) //
                .created(System.currentTimeMillis()) //
                .build();

        dataSetMetadata.getLifecycle().importing(true); // Indicate data set is being imported

        // Save data set content
        LOG.debug(marker, "Storing content...");
        contentStore.storeAsRaw(dataSetMetadata, content);
        LOG.debug(marker, "Content stored.");

        // Create the new data set
        dataSetMetadataRepository.add(dataSetMetadata);
        LOG.debug(marker, "dataset metadata stored {}", dataSetMetadata);

        // Queue events (format analysis, content indexing for search...)
        queueEvents(id);

        // create associated folderEntry
        FolderEntry folderEntry = new FolderEntry(FolderEntry.ContentType.DATASET, id);
        folderRepository.addFolderEntry(folderEntry, folderPath);

        LOG.debug(marker, "Created!");

        return id;
    }

    /**
     * Returns the data set content for given id. Service might return
     * {@link org.apache.commons.httpclient.HttpStatus#SC_ACCEPTED} if the data set exists but analysis is not yet fully
     * completed so content is not yet ready to be served.
     *
     * @param metadata  If <code>true</code>, includes data set metadata information.
     * @param sample    Size of the wanted sample, if missing, the full dataset is returned.
     * @param dataSetId A data set id.
     */
    @RequestMapping(value = "/datasets/{id}/content", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get a data set by id", notes = "Get a data set content based on provided id. Id should be a UUID returned by the list operation. Not valid or non existing data set id returns empty content.")
    @Timed
    @ResponseBody
    public DataSet get(
            @RequestParam(defaultValue = "true") @ApiParam(name = "metadata", value = "Include metadata information in the response") boolean metadata, //
            @RequestParam(required = false) @ApiParam(name = "sample", defaultValue = "0", value = "Size of the wanted sample, if missing, the full dataset is returned") Long sample, //
            @PathVariable(value = "id") @ApiParam(name = "id", value = "Id of the requested data set") String dataSetId) {

        HttpResponseContext.header("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        final Marker marker = Markers.dataset(dataSetId);
        LOG.debug(marker, "Get data set #{}", dataSetId);

        try {
            DataSetMetadata dataSetMetadata = dataSetMetadataRepository.get(dataSetId);
            if (dataSetMetadata == null) {
                HttpResponseContext.status(HttpStatus.NO_CONTENT);
                return DataSet.empty(); // No data set, returns empty content.
            }
            if (dataSetMetadata.getLifecycle().importing()) {
                // Data set is being imported, this is an error since user should not have an id to a being-created
                // data set (create() operation is a blocking operation).
                final ExceptionContext context = ExceptionContext.build().put("id", dataSetId); //$NON-NLS-1$
                throw new TDPException(DataSetErrorCodes.UNABLE_TO_SERVE_DATASET_CONTENT, context);
            }
            // Build the result
            DataSet dataSet = new DataSet();
            if (metadata) {
                completeWithUserData(dataSetMetadata);
                dataSet.setMetadata(dataSetMetadata);
            }

            if (sample != null && sample > 0) {
                // computes the statistics only if columns are required
                if (metadata) {
                    // Compute statistics *before* to avoid consumption of too many threads in serialization (call to a
                    // stream sample may use a thread and a pipe stream, so better to consume to perform in this order).
                    LOG.debug(marker, "Sample statistics...");
                    computeSampleStatistics(dataSetMetadata, sample);
                    LOG.debug(marker, "Sample statistics done.");
                }
                LOG.debug(marker, "Sampling...");
                dataSet.setRecords(contentStore.sample(dataSetMetadata, sample));
                LOG.debug(marker, "Sample done.");
            } else {
                dataSet.setRecords(contentStore.stream(dataSetMetadata));
            }

            return dataSet;

        } finally {
            LOG.debug(marker, "Get done.");
        }
    }

    /**
     * Returns the data set {@link DataSetMetadata metadata} for given <code>dataSetId</code>.
     *
     * @param dataSetId A data set id. If <code>null</code> <b>or</b> if no data set with provided id exits, operation
     *                  returns {@link org.apache.commons.httpclient.HttpStatus#SC_NO_CONTENT}
     */
    @RequestMapping(value = "/datasets/{id}/metadata", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get metadata information of a data set by id", notes = "Get metadata information of a data set by id. Not valid or non existing data set id returns empty content.")
    @Timed
    @ResponseBody
    public DataSet getMetadata(
            @PathVariable(value = "id") @ApiParam(name = "id", value = "Id of the data set metadata") String dataSetId) {
        if (dataSetId == null) {
            HttpResponseContext.status(HttpStatus.NO_CONTENT);
            return null;
        }
        DataSetMetadata metadata = dataSetMetadataRepository.get(dataSetId);
        if (metadata == null) {
            HttpResponseContext.status(HttpStatus.NO_CONTENT);
            return null;
        }
        if (!metadata.getLifecycle().schemaAnalyzed()) {
            HttpResponseContext.status(HttpStatus.ACCEPTED);
            return DataSet.empty();
        }
        DataSet dataSet = new DataSet();
        completeWithUserData(metadata);
        dataSet.setMetadata(metadata);
        return dataSet;
    }

    /**
     * Clone to a new data set and returns the new data set id as text in the response.
     *
     * @param cloneName the name of the cloned dataset
     * @param folderPath the folder path to clone the dataset
     * @return The new data id.
     */
    @RequestMapping(value = "/datasets/clone/{id}", method = PUT, produces = MediaType.TEXT_PLAIN_VALUE)
    @ApiOperation(value = "Clone a data set", produces = MediaType.TEXT_PLAIN_VALUE, notes = "Clone a new data set based on the given id. Returns the id of the newly created data set.")
    @Timed
    public String clone(
            @PathVariable(value = "id") @ApiParam(name = "id", value = "Id of the data set to clone") String dataSetId,
            @ApiParam(value = "The name of the cloned dataset.") @RequestParam(defaultValue = "", required = false) String cloneName,
            @ApiParam(value = "The folder path to create the entry.") @RequestParam(defaultValue = "", required = false) String folderPath)
            throws IOException {

        HttpResponseContext.header("Content-Type", MediaType.TEXT_PLAIN_VALUE);

        DataSetMetadata original = dataSetMetadataRepository.get(dataSetId);
        final DistributedLock lock = dataSetMetadataRepository.createDatasetMetadataLock(dataSetId);
        try {
            lock.lock(); // lock to ensure any asynchronous analysis is completed.
            // if no metadata it's an empty one the get method has already set NO CONTENT http return code
            // so simply return!!
            if (original == null) {
                return StringUtils.EMPTY;
            }
            // use a default name if empty (original name + " Copy" )
            if (StringUtils.isEmpty(cloneName)) {
                cloneName = original.getName() + " Copy";
            }
            // first check if the name is already used in the target folder
            final Iterable<FolderEntry> entries = folderRepository.entries(folderPath, FolderEntry.ContentType.DATASET);
            final String newDatasetName = cloneName;
            entries.forEach(folderEntry -> {
                DataSetMetadata dataSetEntry = dataSetMetadataRepository.get(folderEntry.getContentId());
                if (dataSetEntry != null && StringUtils.equals(newDatasetName, dataSetEntry.getName())) {
                    final ExceptionContext context = ExceptionContext.build() //
                            .put("id", folderEntry.getContentId()) //
                            .put("folder", folderPath) //
                            .put("name", newDatasetName);
                    throw new TDPException(DataSetErrorCodes.DATASET_NAME_ALREADY_USED, context, true);
                }
            });
            // Create copy (based on original data set metadata)
            final String newId = UUID.randomUUID().toString();
            final Marker marker = Markers.dataset(newId);
            LOG.debug(marker, "Cloning...");
            DataSetMetadata target = metadataBuilder.metadata() //
                    .copy(original) //
                    .id(newId) //
                    .name(cloneName) //
                    .author(security.getUserId()) //
                    .location(original.getLocation()) //
                    .created(System.currentTimeMillis()) //
                    .build();
            // Save data set content
            LOG.debug(marker, "Storing content...");
            try (InputStream content = contentStore.getAsRaw(original)) {
                contentStore.storeAsRaw(target, content);
            }
            LOG.debug(marker, "Content stored.");

            // Create the new data set
            dataSetMetadataRepository.add(target);

        // create associated folderEntry
        FolderEntry folderEntry = new FolderEntry(FolderEntry.ContentType.DATASET, newId);
        folderRepository.addFolderEntry(folderEntry, folderPath);

            LOG.debug(marker, "Cloned!");
            return newId;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Move a data set to an other folder
     *
     * @param folderPath    The original folder path of the dataset
     * @param newFolderPath The new folder path of the dataset
     */
    @RequestMapping(value = "/datasets/move/{id}", method = PUT, produces = MediaType.TEXT_PLAIN_VALUE)
    @ApiOperation(value = "Move a data set", produces = MediaType.TEXT_PLAIN_VALUE, notes = "Move a data set to an other folder.")
    @Timed
    public void move(@PathVariable(value = "id") @ApiParam(name = "id", value = "Id of the data set to move") String dataSetId,
                     @ApiParam(value = "The original folder path of the dataset.") @RequestParam(defaultValue = "", required = false) String folderPath,
                     @ApiParam(value = "The new folder path of the dataset.") @RequestParam(defaultValue = "", required = false) String newFolderPath,
                     @ApiParam(value = "The new name of the moved dataset.") @RequestParam(defaultValue = "", required = false) String newName)
            throws IOException {

        HttpResponseContext.header("Content-Type", MediaType.TEXT_PLAIN_VALUE);

        DataSet dataSet = get(true, null, dataSetId);

        final String dataSetName = StringUtils.isEmpty(newName) ? dataSet.getMetadata().getName() : newName;

        // if no metadata it's an empty one the get method has already set NO CONTENT http return code
        // so simply return!!
        if (dataSet.getMetadata() == null) {
            return;
        }
        // first check if the name is already used in the target folder
        final Iterable<FolderEntry> entries = folderRepository.entries(newFolderPath, FolderEntry.ContentType.DATASET);

        entries.forEach(folderEntry -> {
            DataSetMetadata dataSetEntry = dataSetMetadataRepository.get(folderEntry.getContentId());
            if (dataSetEntry != null && StringUtils.equals(dataSetName, dataSetEntry.getName())) {
                final ExceptionContext context = ExceptionContext.build() //
                        .put("id", folderEntry.getContentId()) //
                        .put("folder", folderPath) //
                        .put("name", dataSetName);
                throw new TDPException(DataSetErrorCodes.DATASET_NAME_ALREADY_USED, context, true);
            }
        });

        // rename the dataset only if we received a new name
        if (StringUtils.isNotEmpty(newName) && !StringUtils.equals(newName, dataSet.getMetadata().getName())) {
            DistributedLock datasetLock = dataSetMetadataRepository.createDatasetMetadataLock(dataSetId);
            datasetLock.lock();
            try {
                dataSet.getMetadata().setName(newName);
                dataSetMetadataRepository.add(dataSet.getMetadata());
            } finally {
                datasetLock.unlock();
            }
        }
        FolderEntry folderEntry = new FolderEntry(FolderEntry.ContentType.DATASET, dataSetId);

        folderRepository.moveFolderEntry(folderEntry, folderPath, newFolderPath);
    }

    /**
     * Deletes a data set with provided id.
     *
     * @param dataSetId A data set id. If data set id is unknown, no exception nor status code to indicate this is set.
     */
    @RequestMapping(value = "/datasets/{id}", method = RequestMethod.DELETE, consumes = MediaType.ALL_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    @ApiOperation(value = "Delete a data set by id", notes = "Delete a data set content based on provided id. Id should be a UUID returned by the list operation. Not valid or non existing data set id returns empty content.")
    @Timed
    public void delete(
            @PathVariable(value = "id") @ApiParam(name = "id", value = "Id of the data set to delete") String dataSetId) {
        DataSetMetadata metadata = dataSetMetadataRepository.get(dataSetId);
        final DistributedLock lock = dataSetMetadataRepository.createDatasetMetadataLock(dataSetId);
        try {
            lock.lock();
            if (metadata != null) {
                contentStore.delete(metadata);
                dataSetMetadataRepository.remove(dataSetId);
            } // do nothing if the dataset does not exists
        } finally {
            lock.unlock();
        }

        // delete the associated folder entries
        // TODO make this async?
        for (FolderEntry folderEntry : folderRepository.findFolderEntries(dataSetId, FolderEntry.ContentType.DATASET)) {
            folderRepository.removeFolderEntry(folderEntry.getFolderId(), //
                    folderEntry.getContentId(), //
                    folderEntry.getContentType());
        }

    }

    @RequestMapping(value = "/datasets/{id}/processcertification", method = PUT, consumes = MediaType.ALL_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    @ApiOperation(value = "Ask certification for a dataset", notes = "Advance certification step of this dataset.")
    @Timed
    public void processCertification(
            @PathVariable(value = "id") @ApiParam(name = "id", value = "Id of the data set to update") String dataSetId) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Ask certification for dataset #{}", dataSetId);
        }

        DistributedLock datasetLock = dataSetMetadataRepository.createDatasetMetadataLock(dataSetId);
        datasetLock.lock();
        try {
            DataSetMetadata dataSetMetadata = dataSetMetadataRepository.get(dataSetId);
            if (dataSetMetadata != null) {
                LOG.trace("Current certification step is " + dataSetMetadata.getGovernance().getCertificationStep());

                if (dataSetMetadata.getGovernance().getCertificationStep() == Certification.NONE) {
                    dataSetMetadata.getGovernance().setCertificationStep(Certification.PENDING);
                    dataSetMetadataRepository.add(dataSetMetadata);
                } else if (dataSetMetadata.getGovernance().getCertificationStep() == Certification.PENDING) {
                    dataSetMetadata.getGovernance().setCertificationStep(Certification.CERTIFIED);
                    dataSetMetadataRepository.add(dataSetMetadata);
                } else if (dataSetMetadata.getGovernance().getCertificationStep() == Certification.CERTIFIED) {
                    dataSetMetadata.getGovernance().setCertificationStep(Certification.NONE);
                    dataSetMetadataRepository.add(dataSetMetadata);
                }

                LOG.debug("New certification step is " + dataSetMetadata.getGovernance().getCertificationStep());
            } // else do nothing if the dataset does not exists
        } finally {
            datasetLock.unlock();
        }
    }

    /**
     * Updates a data set content and metadata. If no data set exists for given id, data set is silently created.
     *
     * @param dataSetId      The id of data set to be updated.
     * @param name           The new name for the data set.
     * @param dataSetContent The new content for the data set. If empty, existing content will <b>not</b> be replaced.
     *                       For delete operation, look at {@link #delete(String)}.
     */
    @RequestMapping(value = "/datasets/{id}/raw", method = PUT, consumes = MediaType.ALL_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    @ApiOperation(value = "Update a data set by id", consumes = "text/plain", notes = "Update a data set content based on provided id and PUT body. Id should be a UUID returned by the list operation. Not valid or non existing data set id returns empty content. For documentation purposes, body is typed as 'text/plain' but operation accepts binary content too.")
    @Timed
    @VolumeMetered
    public void updateRawDataSet(
            @PathVariable(value = "id") @ApiParam(name = "id", value = "Id of the data set to update") String dataSetId, //
            @RequestParam(value = "name", required = false) @ApiParam(name = "name", value = "New value for the data set name") String name, //
            @ApiParam(value = "content") InputStream dataSetContent) {
        final DistributedLock lock = dataSetMetadataRepository.createDatasetMetadataLock(dataSetId);
        try {
            lock.lock();
            final DataSetMetadataBuilder datasetBuilder = metadataBuilder.metadata().id(dataSetId);
            final DataSetMetadata metadataForUpdate = dataSetMetadataRepository.get(dataSetId);
            if (metadataForUpdate != null) {
                datasetBuilder.copyNonContentRelated(metadataForUpdate);
                datasetBuilder.modified(System.currentTimeMillis());
            }
            if (name != null) {
                datasetBuilder.name(name);
            }
            final DataSetMetadata dataSetMetadata = datasetBuilder.build();

            // Save data set content
            contentStore.storeAsRaw(dataSetMetadata, dataSetContent);
            dataSetMetadataRepository.add(dataSetMetadata);
        } finally {
            lock.unlock();
        }
        // Content was changed, so queue events (format analysis, content indexing for search...)
        queueEvents(dataSetId);
    }

    /**
     * List all dataset related error codes.
     */
    @RequestMapping(value = "/datasets/errors", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get all dataset related error codes.", notes = "Returns the list of all dataset related error codes.")
    @Timed
    public Iterable<JsonErrorCodeDescription> listErrors() {
        // need to cast the typed dataset errors into mock ones to use json parsing
        List<JsonErrorCodeDescription> errors = new ArrayList<>(DataSetErrorCodes.values().length);
        for (DataSetErrorCodes code : DataSetErrorCodes.values()) {
            errors.add(new JsonErrorCodeDescription(code));
        }
        return errors;
    }

    /**
     * Returns preview of the the data set content for given id (first 100 rows). Service might return
     * {@link org.apache.commons.httpclient.HttpStatus#SC_ACCEPTED} if the data set exists but analysis is not yet fully
     * completed so content is not yet ready to be served.
     *
     * @param metadata  If <code>true</code>, includes data set metadata information.
     * @param sheetName the sheet name to preview
     * @param dataSetId A data set id.
     */
    @RequestMapping(value = "/datasets/{id}/preview", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get a data preview set by id", notes = "Get a data set preview content based on provided id. Not valid or non existing data set id returns empty content. Data set not in drat status will return a redirect 301")
    @Timed
    @ResponseBody
    public DataSet preview(
            @RequestParam(defaultValue = "true") @ApiParam(name = "metadata", value = "Include metadata information in the response") boolean metadata, //
            @RequestParam(defaultValue = "") @ApiParam(name = "sheetName", value = "Sheet name to preview") String sheetName, //
            @PathVariable(value = "id") @ApiParam(name = "id", value = "Id of the requested data set") String dataSetId //
    ) {

        DataSetMetadata dataSetMetadata = dataSetMetadataRepository.get(dataSetId);

        if (dataSetMetadata == null) {
            HttpResponseContext.status(HttpStatus.NO_CONTENT);
            return DataSet.empty(); // No data set, returns empty content.
        }
        if (!dataSetMetadata.isDraft()) {
            // Moved to get data set content operation
            HttpResponseContext.status(HttpStatus.MOVED_PERMANENTLY);
            HttpResponseContext.header("Location", "/datasets/" + dataSetId + "/content");
            return DataSet.empty(); // dataset not anymore a draft so preview doesn't make sense.
        }
        if (StringUtils.isNotEmpty(sheetName)) {
            dataSetMetadata.setSheetName(sheetName);
        }
        // take care of previous data without schema parser result
        if (dataSetMetadata.getSchemaParserResult() != null) {
            // sheet not yet set correctly so use the first one
            if (StringUtils.isEmpty(dataSetMetadata.getSheetName())) {
                String theSheetName = dataSetMetadata.getSchemaParserResult().getSheetContents().get(0).getName();
                LOG.debug("preview for dataSetMetadata: {} with sheetName: {}", dataSetId, theSheetName);
                dataSetMetadata.setSheetName(theSheetName);
            }

            String theSheetName = dataSetMetadata.getSheetName();

            Optional<SchemaParserResult.SheetContent> sheetContentFound = dataSetMetadata.getSchemaParserResult()
                    .getSheetContents().stream().filter(sheetContent -> theSheetName.equals(sheetContent.getName())).findFirst();

            if (!sheetContentFound.isPresent()) {
                HttpResponseContext.status(HttpStatus.NO_CONTENT);
                return DataSet.empty(); // No sheet found, returns empty content.
            }

            List<ColumnMetadata> columnMetadatas = sheetContentFound.get().getColumnMetadatas();

            if (dataSetMetadata.getRowMetadata() == null) {
                dataSetMetadata.setRowMetadata(new RowMetadata(Collections.emptyList()));
            }

            dataSetMetadata.getRowMetadata().setColumns(columnMetadatas);
        } else {
            LOG.warn("dataset#{} has draft status but any SchemaParserResult");
        }
        // Build the result
        DataSet dataSet = new DataSet();
        if (metadata) {
            completeWithUserData(dataSetMetadata);
            dataSet.setMetadata(dataSetMetadata);
        }
        dataSet.setRecords(contentStore.stream(dataSetMetadata).limit(100));
        return dataSet;
    }

    /**
     * This gets the current user data related to the dataSetMetadata and updates the dataSetMetadata accordingly. First
     * check for favorites dataset
     *
     * @param dataSetMetadata, the metadata to be updated
     */
    void completeWithUserData(DataSetMetadata dataSetMetadata) {
        String userId = security.getUserId();
        UserData userData = userDataRepository.get(userId);
        if (userData != null) {
            dataSetMetadata.setFavorite(userData.getFavoritesDatasets().contains(dataSetMetadata.getId()));
        } // no user data related to the current user to do nothing
    }

    /**
     * Updates a data set metadata. If no data set exists for given id, a {@link TDPException} is thrown.
     *
     * @param dataSetId       The id of data set to be updated.
     * @param dataSetMetadata The new content for the data set. If empty, existing content will <b>not</b> be replaced.
     *                        For delete operation, look at {@link #delete(String)}.
     */
    @RequestMapping(value = "/datasets/{id}", method = PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Update a data set metadata by id", consumes = "application/json", notes = "Update a data set metadata according to the content of the PUT body. Id should be a UUID returned by the list operation. Not valid or non existing data set id return an error response.")
    @Timed
    public void updateDataSet(
            @PathVariable(value = "id") @ApiParam(name = "id", value = "Id of the data set to update") String dataSetId,
            @RequestBody DataSetMetadata dataSetMetadata) {
        final DistributedLock lock = dataSetMetadataRepository.createDatasetMetadataLock(dataSetId);
        lock.lock();
        try {
            LOG.debug("updateDataSet: {}", dataSetMetadata);

            //
            // Only part of the metadata can be updated, so the original dataset metadata is loaded and updated
            //
            DataSetMetadata metadataForUpdate = dataSetMetadataRepository.get(dataSetId);
            DataSetMetadata original = metadataBuilder.metadata().copy(metadataForUpdate).build();

            if (metadataForUpdate == null) {
                // No need to silently create the data set metadata: associated content will most likely not exist.
                throw new TDPException(DataSetErrorCodes.DATASET_DOES_NOT_EXIST, ExceptionContext.build().put("id", dataSetId));
            }

            try {
                // update the name
                metadataForUpdate.setName(dataSetMetadata.getName());

                // update the sheet content (in case of a multi-sheet excel file)
                if (metadataForUpdate.getSchemaParserResult() != null) {
                    Optional<SchemaParserResult.SheetContent> sheetContentFound = metadataForUpdate.getSchemaParserResult()
                            .getSheetContents().stream()
                            .filter(sheetContent -> dataSetMetadata.getSheetName().equals(sheetContent.getName())).findFirst();

                    if (sheetContentFound.isPresent()) {
                        List<ColumnMetadata> columnMetadatas = sheetContentFound.get().getColumnMetadatas();
                        if (metadataForUpdate.getRowMetadata() == null) {
                            metadataForUpdate.setRowMetadata(new RowMetadata(Collections.emptyList()));
                        }
                        metadataForUpdate.getRowMetadata().setColumns(columnMetadatas);
                    }

                    metadataForUpdate.setSheetName(dataSetMetadata.getSheetName());
                    metadataForUpdate.setSchemaParserResult(null);
                }

                // update parameters & encoding (so that user can change import parameters for CSV)
                metadataForUpdate.getContent().setParameters(dataSetMetadata.getContent().getParameters());
                metadataForUpdate.setEncoding(dataSetMetadata.getEncoding());

                // update limit
                final Optional<Long> newLimit = dataSetMetadata.getContent().getLimit();
                if (newLimit.isPresent()) {
                    metadataForUpdate.getContent().setLimit(newLimit.get());
                }

                // Validate that the new data set metadata and removes the draft status
                FormatGuess formatGuess = formatGuessFactory.getFormatGuess(dataSetMetadata.getContent().getFormatGuessId());
                try {
                    DraftValidator draftValidator = formatGuess.getDraftValidator();
                    DraftValidator.Result result = draftValidator.validate(dataSetMetadata);
                    if (result.isDraft()) {
                        // This is not an exception case: data set may remain a draft after update (although rather
                        // unusual)
                        LOG.warn("Data set #{} is still a draft after update.", dataSetId);
                        return;
                    }
                    // Data set metadata to update is no longer a draft
                    metadataForUpdate.setDraft(false);
                } catch (UnsupportedOperationException e) {
                    // no need to validate draft here
                }

                // update schema
                formatAnalyzer.update(original, metadataForUpdate);

                // save the result
                dataSetMetadataRepository.add(metadataForUpdate);

                // all good mate!! so send that to jms
                // Asks for a in depth schema analysis (for column type information).
                queueEvents(dataSetId, FormatAnalysis.class);
            } catch (Exception e) {
                throw new TDPException(CommonErrorCodes.UNABLE_TO_PARSE_JSON, e);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * list all the favorites dataset for the current user
     *
     * @return a list of the dataset Ids of all the favorites dataset for the current user or an empty list if none
     * found
     */
    @RequestMapping(value = "/datasets/favorites", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "return all favorites datasets of the current user", notes = "Returns the list of favorites datasets.")
    @Timed
    public Iterable<String> favorites() {
        String userId = security.getUserId();
        UserData userData = userDataRepository.get(userId);
        return userData != null ? userData.getFavoritesDatasets() : Collections.emptyList();
    }

    /**
     * update the current user data dataset favorites list by adding or removing the dataSetId according to the unset
     * flag. The user data for the current will be created if it does not exist. If no data set exists for given id, a
     * {@link TDPException} is thrown.
     *
     * @param unset,     if true this will remove the dataSetId from the list of favorites, if false then it adds the
     *                   dataSetId to the favorite list
     * @param dataSetId, the id of the favorites data set. If the data set does not exists nothing is done.
     */
    @RequestMapping(value = "/datasets/{id}/favorite", method = PUT, consumes = MediaType.ALL_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    @ApiOperation(value = "set or unset a dataset as favorite", notes = "Specify if a dataset is or is not a favorite for the current user.")
    @Timed
    public void setFavorites(
            @RequestParam(defaultValue = "false") @ApiParam(name = "unset", value = "if true then unset the dataset as favorite, if false (default value) set the favorite flag") boolean unset, //
            @PathVariable(value = "id") @ApiParam(name = "id", value = "Id of the favorite data set, do nothing is the id does not exist.") String dataSetId) {
        String userId = security.getUserId();
        // check that dataset exists
        DataSetMetadata dataSetMetadata = dataSetMetadataRepository.get(dataSetId);
        if (dataSetMetadata != null) {
            LOG.debug("{} favorite dataset for #{} for user {}", unset ? "Unset" : "Set", dataSetId, userId); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$

            UserData userData = userDataRepository.get(userId);
            if (unset) {// unset the favorites
                if (userData != null) {
                    userData.getFavoritesDatasets().remove(dataSetId);
                    userDataRepository.save(userData);
                } // no user data for this user so nothing to unset
            } else {// set the favorites
                if (userData == null) {// let's create a new UserData
                    userData = new UserData(userId, versionService.version().getVersionId());
                } // else already created so just update it.
                userData.addFavoriteDataset(dataSetId);
                userDataRepository.save(userData);
            }
        } else {// no dataset found so throws an error
            throw new TDPException(DataSetErrorCodes.DATASET_DOES_NOT_EXIST, ExceptionContext.build().put("id", dataSetId));
        }
    }

    /**
     * Update the column of the data set and computes the
     *
     * @param dataSetId  the dataset id.
     * @param columnId   the column id.
     * @param parameters the new type and domain.
     */
    @RequestMapping(value = "/datasets/{datasetId}/column/{columnId}", method = POST, consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Update a column type and/or domain", consumes = APPLICATION_JSON_VALUE)
    @Timed
    public void updateDatasetColumn(@PathVariable(value = "datasetId")
                                    @ApiParam(name = "datasetId", value = "Id of the dataset")
                                    final String dataSetId, @PathVariable(value = "columnId")
                                    @ApiParam(name = "columnId", value = "Id of the column")
                                    final String columnId, @RequestBody
                                    final UpdateColumnParameters parameters) {

        final DistributedLock lock = dataSetMetadataRepository.createDatasetMetadataLock(dataSetId);
        lock.lock();
        try {

            // check that dataset exists
            final DataSetMetadata dataSetMetadata = dataSetMetadataRepository.get(dataSetId);
            if (dataSetMetadata == null) {
                throw new TDPException(DataSetErrorCodes.DATASET_DOES_NOT_EXIST, ExceptionContext.build().put("id", dataSetId));
            }

            LOG.debug("update dataset column for #{} with type {} and/or domain {}", dataSetId, parameters.getType(),
                    parameters.getDomain());

            // get the column
            final ColumnMetadata column = dataSetMetadata.getRowMetadata().getById(columnId);
            if (column == null) {
                throw new TDPException(DataSetErrorCodes.COLUMN_DOES_NOT_EXIST, //
                        ExceptionContext.build() //
                                .put("id", dataSetId) //
                                .put("columnid", columnId));
            }

            // update type/domain
            if (parameters.getType() != null) {
                column.setType(parameters.getType());
            }
            if (parameters.getDomain() != null) {
                // erase domain to let only type
                if (parameters.getDomain().isEmpty()) {
                    column.setDomain("");
                    column.setDomainLabel("");
                    column.setDomainFrequency(0);
                }
                // change domain
                else {
                    final SemanticDomain semanticDomain = column.getSemanticDomains() //
                            .stream() //
                            .filter(dom -> StringUtils.equals(dom.getId(), parameters.getDomain())) //
                            .findFirst().orElse(null);
                    if (semanticDomain != null) {
                        column.setDomain(semanticDomain.getId());
                        column.setDomainLabel(semanticDomain.getLabel());
                        column.setDomainFrequency(semanticDomain.getFrequency());
                    }
                }
            }

            // save
            dataSetMetadataRepository.add(dataSetMetadata);

            // analyze the updated dataset (not all analysis are performed)
            queueEvents(dataSetId, ContentAnalysis.class, FormatAnalysis.class, SchemaAnalysis.class);

        } finally {
            lock.unlock();
        }
    }

    @RequestMapping(value = "/datasets/encodings", method = GET, consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "list the supported encodings for dataset", notes = "This list can be used by user to change dataset encoding.")
    @Timed
    public List<String> listSupportedEncodings() {
        return encodings.getSupportedCharsets().stream().map(Charset::displayName).collect(Collectors.toList());
    }

    /**
     * Computes quality and statistics for a dataset sample.
     *
     * @param dataSetMetadata the dataset metadata.
     * @param sample          the sample size
     */
    private void computeSampleStatistics(DataSetMetadata dataSetMetadata, long sample) {
        // compute statistics on a copy
        DataSet copy = new DataSet();
        copy.setMetadata(dataSetMetadata);
        // Compute quality and statistics on sample only
        try (Stream<DataSetRow> stream = contentStore.sample(dataSetMetadata, sample)) {
            qualityAnalyzer.computeQuality(copy.getMetadata(), stream, sample);
        }
        try (Stream<DataSetRow> stream = contentStore.sample(dataSetMetadata, sample)) {
            statisticsAnalysis.computeFullStatistics(copy.getMetadata(), stream);
        }
    }

    /**
     * Return a dataset metadata comparator from the given parameters.
     *
     * @param sort the sort key.
     * @param comparisonOrder the order comparator to use.
     * @return a dataset metadata comparator from the given parameters.
     */
    private Comparator<DataSetMetadata> getDataSetMetadataComparator(String sort,
            Comparator<String> comparisonOrder) {
        // Select comparator for sort (either by name or date)
        final Comparator<DataSetMetadata> comparator;
        switch (sort.toUpperCase()) {
        case "NAME":
            comparator = Comparator.comparing(dataSetMetadata -> dataSetMetadata.getName().toUpperCase(), comparisonOrder);
            break;
        case "DATE":
            comparator = Comparator.comparing(dataSetMetadata -> String.valueOf(dataSetMetadata.getCreationDate()),
                    comparisonOrder);
            break;
        default:
            throw new TDPException(CommonErrorCodes.ILLEGAL_ORDER_FOR_LIST, ExceptionContext.build().put("sort", sort));
        }
        return comparator;
    }

    /**
     * Return an order comparator.
     *
     * @param order the order key.
     * @return an order comparator.
     */
    private Comparator<String> getOrderComparator(String order) {
        // Select order (asc or desc)
        final Comparator<String> comparisonOrder;
        switch (order.toUpperCase()) {
        case "ASC":
            comparisonOrder = Comparator.naturalOrder();
            break;
        case "DESC":
            comparisonOrder = Comparator.reverseOrder();
            break;
        default:
            throw new TDPException(CommonErrorCodes.ILLEGAL_ORDER_FOR_LIST, ExceptionContext.build().put("order", order));
        }
        return comparisonOrder;
    }
}
