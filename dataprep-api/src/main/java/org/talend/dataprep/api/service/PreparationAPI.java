//  ============================================================================
//
//  Copyright (C) 2006-2016 Talend Inc. - www.talend.com
//
//  This source code is available under agreement available at
//  https://github.com/Talend/data-prep/blob/master/LICENSE
//
//  You should have received a copy of the agreement
//  along with this program; if not, write to Talend SA
//  9 rue Pages 92150 Suresnes, France
//
//  ============================================================================

package org.talend.dataprep.api.service;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.validation.Valid;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.HttpClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.talend.dataprep.api.preparation.AppendStep;
import org.talend.dataprep.api.preparation.Preparation;
import org.talend.dataprep.api.service.api.PreviewAddInput;
import org.talend.dataprep.api.service.api.PreviewDiffInput;
import org.talend.dataprep.api.service.api.PreviewUpdateInput;
import org.talend.dataprep.api.service.command.dataset.CompatibleDataSetList;
import org.talend.dataprep.api.service.command.preparation.*;
import org.talend.dataprep.exception.TDPException;
import org.talend.dataprep.exception.error.APIErrorCodes;
import org.talend.dataprep.exception.error.CommonErrorCodes;
import org.talend.dataprep.http.HttpResponseContext;
import org.talend.dataprep.metrics.Timed;

import com.netflix.hystrix.HystrixCommand;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
public class PreparationAPI extends APIService {

    @Autowired
    private Jackson2ObjectMapperBuilder builder;

    @RequestMapping(value = "/api/preparations", method = RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get all preparations.", notes = "Returns the list of preparations the current user is allowed to see.")
    @Timed
    public void listPreparations(
            @RequestParam(value = "format", defaultValue = "long") @ApiParam(name = "format", value = "Format of the returned document (can be 'long' or 'short'). Defaults to 'long'.") String format,
            final OutputStream output) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Listing preparations (pool: {} )...", getConnectionStats());
        }
        PreparationList.Format listFormat = PreparationList.Format.valueOf(format.toUpperCase());
        HttpClient client = getClient();
        HystrixCommand<InputStream> command = getCommand(PreparationList.class, client, listFormat);
        try (InputStream commandResult = command.execute()){
            HttpResponseContext.header("Content-Type", APPLICATION_JSON_VALUE); //$NON-NLS-1$
            IOUtils.copyLarge(commandResult, output);
            output.flush();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Listed preparations (pool: {} )...", getConnectionStats());
            }
        } catch (IOException e) {
            throw new TDPException(CommonErrorCodes.UNEXPECTED_EXCEPTION, e);
        }
    }

    /**
     * Returns a list containing all data sets metadata that are compatible with a preparation identified by
     * <tt>preparationId</tt>: its id. If no compatible data set is found an empty list is returned. The base data set
     * of the preparation with id <tt>preparationId</tt> is never returned in the list.
     *
     * @param preparationId the specified preparation id
     * @param sort the sort criterion: either name or date.
     * @param order the sorting order: either asc or desc
     */
    @RequestMapping(value = "/api/preparations/{id}/basedatasets", method = RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get all data sets that are compatible with a preparation.", notes = "Returns the list of data sets the current user is allowed to see and that are compatible with the preparation.")
    @Timed
    public void listCompatibleDatasets(@PathVariable(value = "id") @ApiParam(name = "id", value = "Preparation id.") String preparationId,
            @ApiParam(value = "Sort key (by name or date), defaults to 'date'.") @RequestParam(defaultValue = "DATE", required = false) String sort,
            @ApiParam(value = "Order for sort key (desc or asc), defaults to 'desc'.") @RequestParam(defaultValue = "DESC", required = false) String order,
            final OutputStream output) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Looking for base data set Id (pool: {} )...", getConnectionStats());
        }
        HttpClient client = getClient();

        try {
            // get the preparation
            ByteArrayOutputStream temp = new ByteArrayOutputStream();
            getPreparation(preparationId, temp);
            final String preparationJson = new String(temp.toByteArray());
            if (StringUtils.isEmpty(preparationJson)) {
                throw new TDPException(APIErrorCodes.UNABLE_TO_RETRIEVE_PREPARATION_CONTENT);
            }
            final Preparation preparation = builder.build().readerFor(Preparation.class).readValue(preparationJson);

            // to list compatible datasets
            String dataSetId = preparation.getDataSetId();
            HystrixCommand<InputStream> listCommand = getCommand(CompatibleDataSetList.class, client, dataSetId, sort, order);
            InputStream content = listCommand.execute();
            IOUtils.copyLarge(content, output);
            output.flush();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Listing compatible datasets (pool: {}) done.", getConnectionStats());
            }
        } catch (IOException e) {
            throw new TDPException(APIErrorCodes.UNABLE_TO_LIST_COMPATIBLE_DATASETS, e);
        }
    }

    @RequestMapping(value = "/api/preparations", method = POST, consumes = APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    @ApiOperation(value = "Create a new preparation for preparation content in body.", notes = "Returns the created preparation id.")
    @Timed
    public String createPreparation(
            @ApiParam(name = "body", value = "The original preparation. You may set all values, service will override values you can't write to.") @RequestBody Preparation preparation) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating preparation (pool: {} )...", getConnectionStats());
        }
        HttpClient client = getClient();
        PreparationCreate preparationCreate = getCommand(PreparationCreate.class, client, preparation);
        final String preparationId = preparationCreate.execute();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Created preparation (pool: {} )...", getConnectionStats());
        }
        return preparationId;
    }

    @RequestMapping(value = "/api/preparations/{id}", method = PUT, consumes = APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    @ApiOperation(value = "Update a preparation with content in body.", notes = "Returns the updated preparation id.")
    @Timed
    public String updatePreparation(
            @ApiParam(name = "id", value = "The id of the preparation to update.") @PathVariable("id") String id,
            @ApiParam(name = "body", value = "The updated preparation. Null values are ignored during update. You may set all values, service will override values you can't write to.") @RequestBody Preparation preparation) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Updating preparation (pool: {} )...", getConnectionStats());
        }
        HttpClient client = getClient();
        PreparationUpdate preparationUpdate = getCommand(PreparationUpdate.class, client, id, preparation);
        final String preparationId = preparationUpdate.execute();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Updated preparation (pool: {} )...", getConnectionStats());
        }
        return preparationId;
    }

    @RequestMapping(value = "/api/preparations/{id}", method = DELETE, consumes = MediaType.ALL_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    @ApiOperation(value = "Delete a preparation by id", notes = "Delete a preparation content based on provided id. Id should be a UUID returned by the list operation. Not valid or non existing preparation id returns empty content.")
    @Timed
    public String deletePreparation(
            @ApiParam(name = "id", value = "The id of the preparation to delete.") @PathVariable("id") String id) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Deleting preparation (pool: {} )...", getConnectionStats());
        }
        HttpClient client = getClient();
        PreparationDelete preparationDelete = getCommand(PreparationDelete.class, client, id);
        final String preparationId = preparationDelete.execute();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Deleted preparation (pool: {} )...", getConnectionStats());
        }
        return preparationId;
    }

    @RequestMapping(value = "/api/preparations/clone/{id}", method = PUT, produces = MediaType.TEXT_PLAIN_VALUE)
    @ApiOperation(value = "Clone a preparation by id", notes = "Clone a preparation content based on provided id.")
    @Timed
    public String clonePreparation(
            @ApiParam(name = "id", value = "The id of the preparation to clone.") @PathVariable("id") String id) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Cloning preparation (pool: {} )...", getConnectionStats());
        }
        HttpClient client = getClient();
        PreparationClone preparationClone = getCommand(PreparationClone.class, client, id);
        String preparationId = preparationClone.execute();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Cloned preparation (pool: {} )...", getConnectionStats());
        }
        return preparationId;
    }

    @RequestMapping(value = "/api/preparations/{id}/details", method = RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get a preparation by id and details.", notes = "Returns the preparation details.")
    @Timed
    public void getPreparation(@PathVariable(value = "id") @ApiParam(name = "id", value = "Preparation id.") String preparationId,
            final OutputStream output) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Retrieving preparation details (pool: {} )...", getConnectionStats());
        }
        HttpClient client = getClient();
        HystrixCommand<InputStream> command = getCommand(PreparationGet.class, client, preparationId);
        try (InputStream commandResult = command.execute()) {
            // You cannot use Preparation object mapper here: to serialize steps & actions, you'd need a version
            // repository not available at API level. Code below copies command result direct to response.
            HttpResponseContext.header("Content-Type", APPLICATION_JSON_VALUE); //$NON-NLS-1$
            IOUtils.copyLarge(commandResult, output);
            output.flush();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Retrieved preparation details (pool: {} )...", getConnectionStats());
            }
        } catch (IOException e) {
            throw new TDPException(CommonErrorCodes.UNEXPECTED_EXCEPTION, e);
        }
    }

    @RequestMapping(value = "/api/preparations/{id}/content", method = RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get preparation content by id and at a given version.", notes = "Returns the preparation content at version.")
    @Timed
    public void getPreparation(@PathVariable(value = "id") @ApiParam(name = "id", value = "Preparation id.") String preparationId,
            @RequestParam(value = "version", defaultValue = "head") @ApiParam(name = "version", value = "Version of the preparation (can be 'origin', 'head' or the version id). Defaults to 'head'.") String version,
            @RequestParam(required = false, defaultValue = "full") @ApiParam(name = "sample", value = "Size of the wanted sample, if missing or 'full', the full preparation content is returned") String sample, //
            final OutputStream output) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Retrieving preparation content (pool: {} )...", getConnectionStats());
        }
        HttpClient client = getClient();
        Long sampleValue;
        try {
            sampleValue = Long.parseLong(sample);
        } catch (NumberFormatException e) {
            sampleValue = null;
        }
        HystrixCommand<InputStream> command = getCommand(PreparationGetContent.class, client, preparationId, version,
                sampleValue);
        try (InputStream preparationContent = command.execute()) {
            IOUtils.copyLarge(preparationContent, output);
            output.flush();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Retrieved preparation content (pool: {} )...", getConnectionStats());
            }
        } catch (IOException e) {
            throw new TDPException(CommonErrorCodes.UNEXPECTED_EXCEPTION, e);
        }
    }

    @RequestMapping(value = "/api/preparations/{id}/actions", method = POST, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Adds an action at the end of preparation.", notes = "Does not return any value, client may expect successful operation based on HTTP status code.")
    @Timed
    public void addPreparationAction(@PathVariable(value = "id")
    @ApiParam(name = "id", value = "Preparation id.")
    final String preparationId, @RequestBody
    @ApiParam("Action to add at end of the preparation.")
    final AppendStep step) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Adding action to preparation (pool: {} )...", getConnectionStats());
        }
        final HttpClient client = getClient();
        final HystrixCommand<Void> command = getCommand(PreparationAddAction.class, client, preparationId, step);
        command.execute();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Added action to preparation (pool: {} )...", getConnectionStats());
        }
    }

    @RequestMapping(value = "/api/preparations/{preparationId}/actions/{stepId}", method = PUT, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Updates an action in the preparation.", notes = "Does not return any value, client may expect successful operation based on HTTP status code.")
    @Timed
    public void updatePreparationAction(@PathVariable(value = "preparationId")
    @ApiParam(name = "preparationId", value = "Preparation id.")
    final String preparationId, @PathVariable(value = "stepId")
    @ApiParam(name = "stepId", value = "Step id in the preparation.")
    final String stepId, @RequestBody
    @ApiParam("New content for the action.")
    final AppendStep step) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Updating preparation action at step #{} (pool: {} )...", stepId, //
                    getConnectionStats());
        }
        final HttpClient client = getClient();
        final HystrixCommand<Void> command = getCommand(PreparationUpdateAction.class, client, preparationId, stepId, step);
        command.execute();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Updated preparation action at step #{} (pool: {} )...", stepId, //
                    getConnectionStats());
        }
    }

    @RequestMapping(value = "/api/preparations/{id}/actions/{stepId}", method = DELETE, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Delete an action in the preparation.", notes = "Does not return any value, client may expect successful operation based on HTTP status code.")
    @Timed
    public void deletePreparationAction(@PathVariable(value = "id")
    @ApiParam(name = "id", value = "Preparation id.")
    final String preparationId, @PathVariable(value = "stepId")
    @ApiParam(name = "stepId", value = "Step id to delete.")
    final String stepId) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Deleting preparation action at step #{} (pool: {} ) ...", stepId, //
                    getConnectionStats());
        }
        final HttpClient client = getClient();
        final HystrixCommand<Void> command = getCommand(PreparationDeleteAction.class, client, preparationId, stepId);
        command.execute();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Deleted preparation action at step #{} (pool: {} ) ...", stepId, //
                    getConnectionStats());
        }
    }

    @RequestMapping(value = "/api/preparations/{id}/head/{headId}", method = PUT, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Delete an action in the preparation.", notes = "Does not return any value, client may expect successful operation based on HTTP status code.")
    @Timed
    public void setPreparationHead(@PathVariable(value = "id")
    @ApiParam(name = "id", value = "Preparation id.")
    final String preparationId, @PathVariable(value = "headId")
    @ApiParam(name = "headId", value = "New head step id")
    final String headId) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Moving preparation #{} head to step '{}'...", preparationId, headId);
        }

        final HttpClient client = getClient();
        final HystrixCommand<Void> command = getCommand(PreparationMoveHead.class, client, preparationId, headId);
        command.execute();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Moved preparation #{} head to step '{}'...", preparationId, headId);
        }
    }

    // ---------------------------------------------------------------------------------
    // ----------------------------------------PREVIEW----------------------------------
    // ---------------------------------------------------------------------------------

    @RequestMapping(value = "/api/preparations/preview/diff", method = POST, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get a preview diff between 2 steps of the same preparation.")
    @Timed
    public void previewDiff(@RequestBody
    final PreviewDiffInput input, final OutputStream output) {
        final HystrixCommand<InputStream> transformation = getCommand(PreviewDiff.class, getClient(), input);
        executePreviewCommand(output, transformation);
    }

    @RequestMapping(value = "/api/preparations/preview/update", method = POST, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get a preview diff between the same step of the same preparation but with one step update.")
    public void previewUpdate(@RequestBody
    final PreviewUpdateInput input, final OutputStream output) {
        final HystrixCommand<InputStream> transformation = getCommand(PreviewUpdate.class, getClient(), input);
        executePreviewCommand(output, transformation);
    }

    @RequestMapping(value = "/api/preparations/preview/add", method = POST, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get a preview between the head step and a new appended transformation")
    public void previewAdd(@RequestBody
    @Valid
    final PreviewAddInput input, final OutputStream output) {
        final HystrixCommand<InputStream> transformation = getCommand(PreviewAdd.class, getClient(), input);
        executePreviewCommand(output, transformation);
    }

    private void executePreviewCommand(OutputStream output, HystrixCommand<InputStream> transformation) {
        try (InputStream commandResult = transformation.execute()) {
            IOUtils.copyLarge(commandResult, output);
            output.flush();
            LOG.info("Done executing preview command");
        } catch (IOException e) {
            // Preview commands are very often cancelled by UI, this is not an actual issue.
            LOG.error("Aborted preview due to I/O exception.", e);
        } catch (Exception e) {
            throw new TDPException(APIErrorCodes.UNABLE_TO_TRANSFORM_DATASET, e);
        }
    }
}
