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

package org.talend.dataprep.preparation.service;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.*;
import static org.talend.dataprep.exception.error.PreparationErrorCodes.*;

import java.text.DecimalFormat;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.talend.daikon.exception.ExceptionContext;
import org.talend.dataprep.api.dataset.DataSetMetadata;
import org.talend.dataprep.api.preparation.*;
import org.talend.dataprep.api.service.info.VersionService;
import org.talend.dataprep.exception.TDPException;
import org.talend.dataprep.exception.error.CommonErrorCodes;
import org.talend.dataprep.exception.error.DataSetErrorCodes;
import org.talend.dataprep.exception.error.PreparationErrorCodes;
import org.talend.dataprep.exception.json.JsonErrorCodeDescription;
import org.talend.dataprep.metrics.Timed;
import org.talend.dataprep.preparation.store.PreparationRepository;
import org.talend.dataprep.security.Security;
import org.talend.dataprep.transformation.api.action.metadata.common.ImplicitParameters;
import org.talend.dataprep.transformation.api.action.validation.ActionMetadataValidation;
import org.talend.dataprep.transformation.pipeline.ActionRegistry;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;


@RestController
@Api(value = "preparations", basePath = "/preparations", description = "Operations on preparations")
public class PreparationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreparationService.class);

    @Autowired
    private PreparationRepository preparationRepository;

    @Autowired
    private ActionMetadataValidation validator;

    /** The root step. */
    @Resource(name = "rootStep")
    private Step rootStep;

    @Autowired
    private PreparationUtils preparationUtils;

    /** DataPrep abstraction to the underlying security (whether it's enabled or not). */
    @Autowired
    private Security security;

    @Autowired
    private VersionService versionService;

    @Autowired
    private ActionRegistry actionRegistry;

    @RequestMapping(value = "/preparations", method = GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "List all preparations id", notes = "Returns the list of preparations ids the current user is allowed to see. Creation date is always displayed in UTC time zone. See 'preparations/all' to get all details at once.")
    @Timed
    public List<String> list(
            @ApiParam(value = "Sort key (by name or date).") @RequestParam(defaultValue = "MODIF", required = false) String sort,
            @ApiParam(value = "Order for sort key (desc or asc).") @RequestParam(defaultValue = "DESC", required = false) String order) {
        LOGGER.debug("Get list of preparations (summary).");
        return preparationRepository.listAll(Preparation.class).stream().collect(Collectors.toList()).stream()
                .sorted(getPreparationComparator(sort, getOrderComparator(order))).map(Preparation::id).collect(toList());
    }

    @RequestMapping(value = "/preparations", method = GET, params = "dataSetId", produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "List all preparations for the given DataSet id", notes = "Returns the list of preparations for the given Dataset id the current user is allowed to see. Creation date is always displayed in UTC time zone. See 'preparations/all' to get all details at once.")
    @Timed
    public Collection<PreparationDetails> listByDataSet(@RequestParam("dataSetId") @ApiParam("dataSetId") String dataSetId) {
        Collection<Preparation> preparations = preparationRepository.getByDataSet(dataSetId);
        LOGGER.debug("{} preparation(s) use dataset {}.", preparations.size(), dataSetId);
        return getDetails(preparations);
    }

    @RequestMapping(value = "/preparations", method = GET, params = "name", produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "List all preparations matching the specified name", notes = "Returns the list of preparations matching the specified name. Creation date is always displayed in UTC time zone. See 'preparations/all' to get all details at once.")
    @Timed
    public Collection<PreparationDetails> listByMatchingName(@RequestParam("name") @ApiParam("name") String name,
            @RequestParam(value = "exactMatch", defaultValue = "true") @ApiParam("exactMatch") boolean exactMatch) {
        Collection<Preparation> preparations = preparationRepository.getByMatchingName(name, exactMatch);

        String message = exactMatch ? "{} preparation(s) having name that match {}.": "{} preparation(s) having name containing {}.";
        LOGGER.debug(message, preparations.size(), name);
        return getDetails(preparations);
    }

    @RequestMapping(value = "/preparations/all", method = GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "List all preparations", notes = "Returns the list of preparations the current user is allowed to see. Creation date is always displayed in UTC time zone. This operation return all details on the preparations.")
    @Timed
    public Collection<PreparationDetails> listAll(
            @ApiParam(value = "Sort key (by name or date).") @RequestParam(defaultValue = "MODIF", required = false) String sort,
            @ApiParam(value = "Order for sort key (desc or asc).") @RequestParam(defaultValue = "DESC", required = false) String order) {
        LOGGER.debug("Get list of preparations (with details).");
        Collection<Preparation> preparations = preparationRepository.listAll(Preparation.class);
        preparations = preparations.stream()
                // sort by last modification date
                .sorted((p1, p2) -> Long.compare(p2.getLastModificationDate(),p1.getLastModificationDate()))
                .collect(Collectors.toList());
        return getDetails(preparations);
    }

    @RequestMapping(value = "/preparations", method = PUT, produces = TEXT_PLAIN_VALUE, consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Create a preparation", notes = "Returns the id of the created preparation.")
    @Timed
    public String create(@ApiParam("preparation")
    @RequestBody
    final Preparation preparation) {
        LOGGER.debug("Create new preparation for data set {}", preparation.getDataSetId());
        preparation.setHeadId(rootStep.id());
        preparation.setAuthor(security.getUserId());
        preparation.setAppVersion(versionService.version().getVersionId());
        preparationRepository.add(preparation);
        LOGGER.debug("Created new preparation: {}", preparation);
        return preparation.id();
    }

    @RequestMapping(value = "/preparations/{id}", method = RequestMethod.DELETE, consumes = MediaType.ALL_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    @ApiOperation(value = "Delete a preparation by id", notes = "Delete a preparation content based on provided id. Id should be a UUID returned by the list operation. Not valid or non existing preparation id returns empty content.")
    @Timed
    public void delete(@PathVariable(value = "id") @ApiParam(name = "id", value = "Id of the preparation to delete") String id) {
        LOGGER.debug("Deletion of preparation #{} requested.", id);
        Preparation preparationToDelete = preparationRepository.get(id, Preparation.class);
        preparationRepository.remove(preparationToDelete);
        LOGGER.debug("Deletion of preparation #{} done.", id);
    }

    @RequestMapping(value = "/preparations/{id}", method = PUT, produces = TEXT_PLAIN_VALUE, consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Create a preparation", notes = "Returns the id of the updated preparation.")
    @Timed
    public String update(@ApiParam("id") @PathVariable("id") String id, //
            @RequestBody
    @ApiParam("preparation")
    final Preparation preparation) {
        Preparation previousPreparation = preparationRepository.get(id, Preparation.class);
        LOGGER.debug("Updating preparation with id {}: {}", preparation.id(), previousPreparation);
        Preparation updated = previousPreparation.merge(preparation);
        if (!updated.id().equals(id)) {
            preparationRepository.remove(previousPreparation);
        }
        updated.setAppVersion(versionService.version().getVersionId());
        preparationRepository.add(updated);
        LOGGER.debug("Updated preparation: {}", updated);
        return updated.id();
    }

    @RequestMapping(value = "/preparations/{id}", method = GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get preparation details", notes = "Return the details of the preparation with provided id.")
    @Timed
    public PreparationDetails get(@ApiParam("id") @PathVariable("id") String id) {
        LOGGER.debug("Get content of preparation details for #{}.", id);
        final Preparation preparation = preparationRepository.get(id, Preparation.class);
        return getDetails(preparation);
    }

    @RequestMapping(value = "/preparations/clone/{id}", method = PUT, produces = TEXT_PLAIN_VALUE)
    @ApiOperation(value = "Clone preparation", notes = "Clone of the preparation with provided id. The new name will the previous one concat with ' Copy', "
            + "Return the id of the new preparation ")
    @Timed
    public String clone(@ApiParam("id") @PathVariable("id") String id) {
        LOGGER.debug("Clone preparation  #{}.", id);
        Preparation preparation = preparationRepository.get(id, Preparation.class);
        preparation.setName(preparation.getName() + " Copy");
        preparation.setCreationDate(System.currentTimeMillis());
        preparation.setLastModificationDate(System.currentTimeMillis());
        preparationRepository.add(preparation);
        return preparation.getId();
    }

    @RequestMapping(value = "/preparations/{id}/steps", method = GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get all preparation steps id", notes = "Return the steps of the preparation with provided id.")
    @Timed
    public List<String> getSteps(@ApiParam("id") @PathVariable("id") String id) {
        LOGGER.debug("Get steps of preparation for #{}.", id);
        final Step step = getStep(id);
        return preparationUtils.listStepsIds(step.id(), preparationRepository);
    }

    /**
     * Append step(s) in a preparation.
     */
    @RequestMapping(value = "/preparations/{id}/actions", method = POST, consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Adds an action to a preparation", notes = "Append an action at end of the preparation with given id.")
    @Timed
    public void appendSteps(@PathVariable("id")
    final String id, @RequestBody
    final AppendStep stepsToAppend) {
        checkActionStepConsistency(stepsToAppend);

        LOGGER.debug("Adding actions to preparation #{}", id);

        final Preparation preparation = getPreparation(id);
        LOGGER.debug("Current head for preparation #{}: {}", id, preparation.getHeadId());

        final List<AppendStep> actionsSteps = new ArrayList<>(1);
        actionsSteps.add(stepsToAppend);

        // rebuild history from head
        replaceHistory(preparation, preparation.getHeadId(), actionsSteps);
        LOGGER.debug("Added head to preparation #{}: head is now {}", id, preparation.getHeadId());
    }

    /**
     * Update a step in a preparation <b>Strategy</b><br/>
     * The goal here is to rewrite the preparation history from 'the step to modify' (STM) to the head, with STM
     * containing the new action.<br/>
     * <ul>
     * <li>1. Extract the actions from STM (excluded) to the head</li>
     * <li>2. Insert the new actions before the other extracted actions. The actions list contains all the actions from
     * the <b>NEW</b> STM to the head</li>
     * <li>3. Set preparation head to STM's parent, so STM will be excluded</li>
     * <li>4. Append each action (one step is created by action) after the new preparation head</li>
     * </ul>
     */
    @RequestMapping(value = "/preparations/{id}/actions/{stepId}", method = PUT, consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Updates an action in a preparation", notes = "Modifies an action in preparation's steps.")
    @Timed
    public void updateAction(@PathVariable("id")
    final String preparationId, //
            @PathVariable("stepId")
    final String stepToModifyId, //
            @RequestBody
    final AppendStep newStep) {

        checkActionStepConsistency(newStep);

        LOGGER.debug("Modifying actions in preparation #{}", preparationId);
        final Preparation preparation = getPreparation(preparationId);
        LOGGER.debug("Current head for preparation #{}: {}", preparationId, preparation.getHeadId());

        // Get steps from "step to modify" to the head
        final List<String> steps = extractSteps(preparation, stepToModifyId); // throws an exception if stepId is not in
                                                                              // the preparation
        LOGGER.debug("Rewriting history for {} steps.", steps.size());

        // Extract created columns ids diff infos
        final Step stm = getStep(stepToModifyId);
        final List<String> originalCreatedColumns = stm.getDiff().getCreatedColumns();
        final List<String> updatedCreatedColumns = newStep.getDiff().getCreatedColumns();
        final List<String> deletedColumns = originalCreatedColumns.stream() // columns that the step was creating but
                                                                            // not anymore
                .filter(id -> !updatedCreatedColumns.contains(id)).collect(toList());
        final int columnsDiffNumber = updatedCreatedColumns.size() - originalCreatedColumns.size();
        final int maxCreatedColumnIdBeforeUpdate = !originalCreatedColumns.isEmpty() ? originalCreatedColumns.stream().mapToInt(Integer::parseInt).max().getAsInt() : MAX_VALUE;

        // Build list of actions from modified one to the head
        final List<AppendStep> actionsSteps = getStepsWithShiftedColumnIds(steps, stepToModifyId, deletedColumns,
                maxCreatedColumnIdBeforeUpdate, columnsDiffNumber);
        actionsSteps.add(0, newStep);

        // Rebuild history from modified step
        final Step stepToModify = getStep(stepToModifyId);
        replaceHistory(preparation, stepToModify.getParent(), actionsSteps);
        LOGGER.debug("Modified head of preparation #{}: head is now {}", preparation.getHeadId());
    }

    /**
     * Delete a step in a preparation.<br/>
     * STD : Step To Delete <br/>
     * <br/>
     * <ul>
     * <li>1. Extract the actions from STD (excluded) to the head. The actions list contains all the actions from the
     * STD's child to the head.</li>
     * <li>2. Filter the preparations that apply on a column created by the step to delete. Those steps will be removed
     * too.</li>
     * <li>2bis. Change the actions that apply on columns > STD last created column id. The created columns ids after
     * the STD are shifted.</li>
     * <li>3. Set preparation head to STD's parent, so STD will be excluded</li>
     * <li>4. Append each action after the new preparation head</li>
     * </ul>
     */
    @RequestMapping(value = "/preparations/{id}/actions/{stepId}", method = DELETE)
    @ApiOperation(value = "Delete an action in a preparation", notes = "Delete a step and all following steps from a preparation")
    @Timed
    public void deleteAction(@PathVariable("id")
    final String id, //
            @PathVariable("stepId")
    final String stepToDeleteId) {
        if (rootStep.getId().equals(stepToDeleteId)) {
            throw new TDPException(PREPARATION_ROOT_STEP_CANNOT_BE_DELETED);
        }

        // get steps from 'step to delete' to head
        final Preparation preparation = getPreparation(id);
        final List<String> steps = extractSteps(preparation, stepToDeleteId); // throws an exception if stepId is not in
                                                                              // the preparation

        // get created columns by step to delete
        final Step std = getStep(stepToDeleteId);
        final List<String> deletedColumns = std.getDiff().getCreatedColumns();
        final int columnsDiffNumber = -deletedColumns.size();
        final int maxCreatedColumnIdBeforeUpdate = deletedColumns.isEmpty() ? MAX_VALUE
                : deletedColumns.stream().mapToInt(Integer::parseInt).max().getAsInt();

        LOGGER.debug("Deleting actions in preparation #{} at step #{}", id, stepToDeleteId); //$NON-NLS-1$

        // get new actions to rewrite history from deleted step
        final List<AppendStep> actions = getStepsWithShiftedColumnIds(steps, stepToDeleteId, deletedColumns,
                maxCreatedColumnIdBeforeUpdate, columnsDiffNumber);

        // rewrite history
        final Step stepToDelete = getStep(stepToDeleteId);
        replaceHistory(preparation, stepToDelete.getParent(), actions);
    }

    @RequestMapping(value = "/preparations/{id}/head/{headId}", method = PUT)
    @ApiOperation(value = "Move preparation head", notes = "Set head to the specified head id")
    @Timed
    public void setPreparationHead(@PathVariable("id")
    final String preparationId, //
            @PathVariable("headId")
    final String headId) {

        final Step head = getStep(headId);
        if (head == null) {
            throw new TDPException(PREPARATION_STEP_DOES_NOT_EXIST,
                    ExceptionContext.build().put("id", preparationId).put("stepId", headId));
        }

        final Preparation preparation = getPreparation(preparationId);
        setPreparationHead(preparation, head);
    }

    @RequestMapping(value = "/preparations/{id}/actions/{version}", method = GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get all the actions of a preparation at given version.", notes = "Returns the action JSON at version.")
    @Timed
    public List<Action> getVersionedAction(@ApiParam("id")
    @PathVariable("id")
    final String id, //
            @ApiParam("version")
    @PathVariable("version")
    final String version) {
        LOGGER.debug("Get list of actions of preparations #{} at version {}.", id, version);
        final Preparation preparation = preparationRepository.get(id, Preparation.class);
        if (preparation != null) {
            final String stepId = getStepId(version, preparation);
            final Step step = getStep(stepId);
            return getActions(step);
        } else {
            throw new TDPException(PREPARATION_DOES_NOT_EXIST, ExceptionContext.build().put("id", id));
        }
    }

    /**
     * List all preparation related error codes.
     */
    @RequestMapping(value = "/preparations/errors", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get all preparation related error codes.", notes = "Returns the list of all preparation related error codes.")
    @Timed
    public Iterable<JsonErrorCodeDescription> listErrors() {
        // need to cast the typed dataset errors into mock ones to use json parsing
        List<JsonErrorCodeDescription> errors = new ArrayList<>(PreparationErrorCodes.values().length);
        for (PreparationErrorCodes code : PreparationErrorCodes.values()) {
            errors.add(new JsonErrorCodeDescription(code));
        }
        return errors;
    }

    // ------------------------------------------------------------------------------------------------------------------
    // ------------------------------------------------GETTERS/EXTRACTORS------------------------------------------------
    // ------------------------------------------------------------------------------------------------------------------

    /**
     * Get the actual step id by converting "head" and "origin" to the hash
     *
     * @param version The version to convert to step id
     * @param preparation The preparation
     * @return The converted step Id
     */
    private String getStepId(final String version, final Preparation preparation) {
        if ("head".equalsIgnoreCase(version)) { //$NON-NLS-1$
            return preparation.getHeadId();
        } else if ("origin".equalsIgnoreCase(version)) { //$NON-NLS-1$
            return rootStep.id();
        }
        return version;
    }

    /**
     * Get actions list from root to the provided step
     *
     * @param step The step
     * @return The list of actions
     */
    private List<Action> getActions(final Step step) {
        return new ArrayList<>(preparationRepository.get(step.getContent(), PreparationActions.class).getActions());
    }

    /**
     * Get the step from id
     * 
     * @param stepId The step id
     * @return Le step with the provided id
     */
    private Step getStep(final String stepId) {
        return preparationRepository.get(stepId, Step.class);
    }

    /**
     * Get preparation from id
     *
     * @param id The preparation id.
     * @return The preparation with the provided id
     * @throws TDPException when no preparation has the provided id
     */
    private Preparation getPreparation(final String id) {
        final Preparation preparation = preparationRepository.get(id, Preparation.class);
        if (preparation == null) {
            LOGGER.error("Preparation #{} does not exist", id);
            throw new TDPException(PREPARATION_DOES_NOT_EXIST, ExceptionContext.build().put("id", id));
        }
        return preparation;
    }

    /**
     * Extract all actions after a provided step
     *
     * @param stepsIds The steps list
     * @param afterStep The (excluded) step id where to start the extraction
     * @return The actions after 'afterStep' to the end of the list
     */
    private List<AppendStep> extractActionsAfterStep(final List<String> stepsIds, final String afterStep) {
        final int stepIndex = stepsIds.indexOf(afterStep);
        if (stepIndex == -1) {
            return emptyList();
        }

        final List<Step> steps;
        try (IntStream range = IntStream.range(stepIndex, stepsIds.size())) {
            steps = range.mapToObj(index -> getStep(stepsIds.get(index)))
                    .collect(toList());
        }

        final List<List<Action>> stepActions = steps.stream().map(this::getActions).collect(toList());

        try (IntStream filteredActions = IntStream.range(1, steps.size())) {
            return filteredActions.mapToObj(index -> {
                final List<Action> previous = stepActions.get(index - 1);
                final List<Action> current = stepActions.get(index);
                final Step step = steps.get(index);

                final AppendStep appendStep = new AppendStep();
                appendStep.setDiff(step.getDiff());
                appendStep.setActions(current.subList(previous.size(), current.size()));

                return appendStep;
            }).collect(toList());
        }
    }

    /**
     * Get the steps ids from a specific step to the head. The specific step MUST be defined as an existing step of the
     * preparation
     *
     * @param preparation The preparation
     * @param fromStepId The starting step id
     * @return The steps ids from 'fromStepId' to the head
     * @throws TDPException If 'fromStepId' is not a step of the provided preparation
     */
    private List<String> extractSteps(final Preparation preparation, final String fromStepId) {
        final List<String> steps = preparationUtils.listStepsIds(preparation.getHeadId(), fromStepId, preparationRepository);
        if (!fromStepId.equals(steps.get(0))) {
            throw new TDPException(PREPARATION_STEP_DOES_NOT_EXIST,
                    ExceptionContext.build().put("id", preparation.getId()).put("stepId", fromStepId));
        }
        return steps;
    }

    // ------------------------------------------------------------------------------------------------------------------
    // -----------------------------------------------------CHECKERS-----------------------------------------------------
    // ------------------------------------------------------------------------------------------------------------------

    /**
     * Test if the stepId is the preparation head. Null, "head", "origin" and the actual step id are considered to be
     * the head
     *
     * @param preparation The preparation to test
     * @param stepId The step id to test
     * @return True if 'stepId' is considered as the preparation head
     */
    private boolean isPreparationHead(final Preparation preparation, final String stepId) {
        return stepId == null || "head".equals(stepId) || "origin".equals(stepId) || preparation.getHeadId().equals(stepId);
    }

    /**
     * Check the action parameters consistency
     *
     * @param step the step to check
     */
    private void checkActionStepConsistency(final AppendStep step) {
        for (final Action stepAction : step.getActions()) {
            validator.checkScopeConsistency(actionRegistry.get(stepAction.getName()), stepAction.getParameters());
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    // -----------------------------------------------------HISTORY------------------------------------------------------
    // ------------------------------------------------------------------------------------------------------------------
    /**
     * Currently, the columns ids are generated sequentially. There are 2 cases where those ids change in a step :
     * <ul>
     * <li>1. when a step that creates columns is deleted (ex1 : columns '0009' and '0010').</li>
     * <li>2. when a step that creates columns is updated : it can create more (add) or less (remove) columns. (ex2 :
     * add column '0009', '0010' + '0011' --> add 1 column)</li>
     * </ul>
     * In those cases, we have to
     * <ul>
     * <li>remove all steps that has action on a deleted column</li>
     * <li>shift all columns created after this step (ex1: columns > '0010', ex2: columns > '0011') by the number of
     * columns diff (ex1: remove 2 columns --> shift -2, ex2: add 1 column --> shift +1)</li>
     * <li>shift all actions that has one of the deleted columns as parameter (ex1: columns > '0010', ex2: columns >
     * '0011') by the number of columns diff (ex1: remove 2 columns --> shift -2, ex2: add 1 column --> shift +1)</li>
     * </ul>
     *
     * 1. Get the steps with ids after 'afterStepId' 2. Rule 1 : Remove (filter) the steps which action is on one of the
     * 'deletedColumns' 3. Rule 2 : For all actions on columns ids > 'shiftColumnAfterId', we shift the column_id
     * parameter with a 'columnShiftNumber' value. (New_column_id = column_id + columnShiftNumber, only if column_id >
     * 'shiftColumnAfterId') 4. Rule 3 : The columns created AFTER 'shiftColumnAfterId' are shifted with the same rules
     * as rule 2. (New_created_column_id = created_column_id + columnShiftNumber, only if created_column_id >
     * 'shiftColumnAfterId')
     * 
     * @param stepsIds The steps ids
     * @param afterStepId The (EXCLUDED) step where the extraction starts
     * @param deletedColumns The column ids that will be removed
     * @param shiftColumnAfterId The (EXCLUDED) column id where we start the shift
     * @param shiftNumber The shift number. new_column_id = old_columns_id + columnShiftNumber
     * @return The adapted steps
     */
    private List<AppendStep> getStepsWithShiftedColumnIds(final List<String> stepsIds, final String afterStepId,
            final List<String> deletedColumns, final int shiftColumnAfterId, final int shiftNumber) {
        Stream<AppendStep> stream = extractActionsAfterStep(stepsIds, afterStepId).stream();

        // rule 1 : remove all steps that modify one of the created columns
        if (!deletedColumns.isEmpty()) {
            stream = stream.filter(stepColumnIsNotIn(deletedColumns));
        }

        // when there is nothing to shift, we just return the filtered steps to avoid extra code
        if (shiftNumber == 0) {
            return stream.collect(toList());
        }

        // rule 2 : we have to shift all columns ids created after the step to delete/modify, in the column_id
        // parameters
        // For example, if the step to delete/modify creates columns 0010 and 0011, all steps that apply to column 0012
        // should now apply to 0012 - (2 created columns) = 0010
        stream = stream.map(shiftStepParameter(shiftColumnAfterId, shiftNumber));

        // rule 3 : we have to shift all columns ids created after the step to delete, in the steps diff
        stream = stream.map(shiftCreatedColumns(shiftColumnAfterId, shiftNumber));

        return stream.collect(toList());
    }

    /**
     * When the step diff created column ids > 'shiftColumnAfterId', we shift it by +columnShiftNumber (that wan be
     * negative)
     * 
     * @param shiftColumnAfterId The shift is performed if created column id > shiftColumnAfterId
     * @param shiftNumber The number to shift (can be negative)
     * @return The same step but modified
     */
    private Function<AppendStep, AppendStep> shiftCreatedColumns(final int shiftColumnAfterId, final int shiftNumber) {

        final DecimalFormat format = new DecimalFormat("0000"); //$NON-NLS-1$
        return step -> {
            final List<String> stepCreatedCols = step.getDiff().getCreatedColumns();
            final List<String> shiftedStepCreatedCols = stepCreatedCols.stream().map(colIdStr -> {
                final int columnId = Integer.parseInt(colIdStr);
                if (columnId > shiftColumnAfterId) {
                    return format.format(columnId + (long) shiftNumber);
                }
                return colIdStr;
            }).collect(toList());
            step.getDiff().setCreatedColumns(shiftedStepCreatedCols);
            return step;
        };
    }

    /**
     * When the step column_id parameter > 'shiftColumnAfterId', we shift it by +columnShiftNumber (that wan be
     * negative)
     * 
     * @param shiftColumnAfterId The shift is performed if column id > shiftColumnAfterId
     * @param shiftNumber The number to shift (can be negative)
     * @return The same step but modified
     */
    private Function<AppendStep, AppendStep> shiftStepParameter(final int shiftColumnAfterId, final int shiftNumber) {
        final DecimalFormat format = new DecimalFormat("0000"); //$NON-NLS-1$
        return step -> {
            final Action firstAction = step.getActions().get(0);
            final Map<String, String> parameters = firstAction.getParameters();
            final int columnId = Integer.parseInt(parameters.get(ImplicitParameters.COLUMN_ID.getKey()));
            if (columnId > shiftColumnAfterId) {
                parameters.put("column_id", format.format(columnId + (long) shiftNumber)); //$NON-NLS-1$
            }
            return step;
        };
    }

    /***
     * Predicate that returns if a step action is NOT on one of the columns list
     * 
     * @param columns The columns ids list
     */
    private Predicate<AppendStep> stepColumnIsNotIn(final List<String> columns) {
        return step -> {
            final String columnId = step.getActions().get(0).getParameters().get("column_id"); //$NON-NLS-1$
            return columnId == null || !columns.contains(columnId);
        };
    }

    /**
     * Update the head step of a preparation
     *
     * @param preparation The preparation to update
     * @param head The head step
     */
    private void setPreparationHead(final Preparation preparation, final Step head) {
        preparation.setHeadId(head.id());
        preparation.updateLastModificationDate();
        preparationRepository.add(preparation);
    }

    /**
     * Rewrite the preparation history from a specific step, with the provided actions
     *
     * @param preparation The preparation
     * @param startStepId The step id to start the (re)write. The following steps will be erased
     * @param actionsSteps The actions to perform
     */
    private void replaceHistory(final Preparation preparation, final String startStepId, final List<AppendStep> actionsSteps) {
        // move preparation head to the starting step
        if (!isPreparationHead(preparation, startStepId)) {
            final Step startingStep = getStep(startStepId);
            setPreparationHead(preparation, startingStep);
        }

        actionsSteps.stream().forEach(step -> appendStepToHead(preparation, step));
    }

    /**
     * Append a single step after the preparation head
     * 
     * @param preparation The preparation
     * @param step The step to apply
     */
    private void appendStepToHead(final Preparation preparation, final AppendStep step) {
        // Add new actions after head
        final Step head = preparationRepository.get(preparation.getHeadId(), Step.class);
        final PreparationActions headContent = preparationRepository.get(head.getContent(), PreparationActions.class);
        final PreparationActions newContent = headContent.append(step.getActions());
        preparationRepository.add(newContent);

        // Create new step from new content
        final Step newStep = new Step(head.id(), newContent.id(), versionService.version().getVersionId(), step.getDiff());
        preparationRepository.add(newStep);

        // Update preparation head step
        setPreparationHead(preparation, newStep);
    }

    /**
     * Convenient method to convert &lt;Collection>Preparation&gt; to Collection&lt;PreparationDetails&gt;.
     *
     * @param preparations the preparations to cast.
     * @return the preparations details that match the given preparations.
     */
    private Collection<PreparationDetails> getDetails(Collection<Preparation> preparations) {
        return preparations.stream().map(this::getDetails).collect(Collectors.toList()); // list keeps the order
    }

    /**
     * Convenient method to convert Preparation to PreparationDetails.
     *
     * @param preparation the preparation to cast.
     * @return the preparation details that matches the given preparation.
     */
    private PreparationDetails getDetails(Preparation preparation) {
        return new PreparationDetails(preparation);
    }

    // ------------------------------------------------------------------------------------------------------------------
    // -----------------------------------------------------COMPARATORS------------------------------------------------------
    // ------------------------------------------------------------------------------------------------------------------

    /**
     * Return a dataset metadata comparator from the given parameters.
     *
     * @param sort the sort key.
     * @param comparisonOrder the order comparator to use.
     * @return a dataset metadata comparator from the given parameters.
     */
    private Comparator<Preparation> getPreparationComparator(String sort, Comparator<String> comparisonOrder) {
        // Select comparator for sort (either by name or date)
        final Comparator<Preparation> comparator;
        switch (sort.toUpperCase()) {
        case "NAME":
            comparator = Comparator.comparing(Preparation::getName, comparisonOrder);
            break;
        case "DATE":
            comparator = Comparator.comparing(p -> String.valueOf(p.getCreationDate()), comparisonOrder);
            break;
        case "MODIF":
            comparator = Comparator.comparing(p -> String.valueOf(p.getLastModificationDate()), comparisonOrder);
            break;
        default:
            throw new TDPException(CommonErrorCodes.ILLEGAL_SORT_FOR_LIST, ExceptionContext.build().put("sort", sort));
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
