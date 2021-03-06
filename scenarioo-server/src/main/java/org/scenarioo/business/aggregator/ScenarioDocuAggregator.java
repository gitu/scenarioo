/* scenarioo-server
 * Copyright (C) 2014, scenarioo.org Development Team
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.scenarioo.business.aggregator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import lombok.Data;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.scenarioo.api.ScenarioDocuReader;
import org.scenarioo.api.exception.ResourceNotFoundException;
import org.scenarioo.dao.aggregates.ScenarioDocuAggregationDAO;
import org.scenarioo.dao.configuration.ConfigurationDAO;
import org.scenarioo.model.docu.aggregates.branches.BuildIdentifier;
import org.scenarioo.model.docu.aggregates.branches.BuildImportStatus;
import org.scenarioo.model.docu.aggregates.branches.BuildImportSummary;
import org.scenarioo.model.docu.aggregates.objects.LongObjectNamesResolver;
import org.scenarioo.model.docu.aggregates.scenarios.PageSteps;
import org.scenarioo.model.docu.aggregates.scenarios.ScenarioPageSteps;
import org.scenarioo.model.docu.aggregates.usecases.PageVariantsCounter;
import org.scenarioo.model.docu.aggregates.usecases.UseCaseScenarios;
import org.scenarioo.model.docu.aggregates.usecases.UseCaseScenariosList;
import org.scenarioo.model.docu.derived.BuildLink;
import org.scenarioo.model.docu.entities.Page;
import org.scenarioo.model.docu.entities.Scenario;
import org.scenarioo.model.docu.entities.ScenarioCalculatedData;
import org.scenarioo.model.docu.entities.Step;
import org.scenarioo.model.docu.entities.StepDescription;
import org.scenarioo.model.docu.entities.StepIdentification;
import org.scenarioo.model.docu.entities.UseCase;
import org.scenarioo.model.docu.entities.generic.ObjectReference;

/**
 * The aggregator reads the input docu files of a build and generates the aggregated docu files with additional
 * precalculated data (like indexes etc.).
 * 
 * Make sure to adjust the value of {@link ScenarioDocuAggregator#CURRENT_FILE_FORMAT_VERSION} when the format of
 * generated data is extended or changed.
 * 
 * TODO: Make aggregator more fail safe ... let him continue in case of exceptions or unexpected data (null pointers?)
 * to aggregate at least that part of a documentation build that is okay, such that this part can be accessed and read.
 */
public class ScenarioDocuAggregator {
	
	/**
	 * Version of the file format in filesystem. The data aggregator checks whether the file format is the same,
	 * otherwise the data has to be recalculated.
	 */
	public static final String CURRENT_FILE_FORMAT_VERSION = "0.22";
	
	private final static Logger LOGGER = Logger.getLogger(ScenarioDocuAggregator.class);
	
	private final ScenarioDocuReader reader = new ScenarioDocuReader(ConfigurationDAO.getDocuDataDirectoryPath());
	
	private final LongObjectNamesResolver longObjectNamesResolver = new LongObjectNamesResolver();
	
	private final ScenarioDocuAggregationDAO dao = new ScenarioDocuAggregationDAO(
			ConfigurationDAO.getDocuDataDirectoryPath(), longObjectNamesResolver);
	
	private final Map<String, StepVariantState> mapOfStepVariant = new HashMap<String, StepVariantState>();
	
	private ObjectRepository objectRepository;
	
	@Data
	public static class StepVariantState {
		private StepIdentification firstStep;
		private StepIdentification previousStep;
		private Integer counter;
		
		public StepVariantState(final StepIdentification firstStep) {
			this.firstStep = firstStep;
			this.counter = new Integer(0);
		}
		
		public void increaseCounter() {
			counter++;
		}
	}
	
	public boolean containsAggregatedDataForBuild(final String branchName, final String buildName) {
		String version = dao.loadVersion(branchName, buildName);
		return !StringUtils.isBlank(version)
				&& version.equals(CURRENT_FILE_FORMAT_VERSION);
	}
	
	public void removeAggregatedDataForBuild(final String branchName,
			final String buildName) {
		dao.deleteDerivedFiles(branchName, buildName);
		objectRepository = new ObjectRepository(branchName, buildName, dao);
		objectRepository.removeAnyExistingObjectData();
	}
	
	public void calculateAggregatedDataForBuild(final String branchName,
			final String buildName) {
		
		objectRepository = new ObjectRepository(branchName, buildName, dao);
		objectRepository.removeAnyExistingObjectData();
		
		LOGGER.info("  calculating aggregated data for build : " + buildName);
		UseCaseScenariosList useCaseScenariosList = calculateUseCaseScenariosList(
				branchName, buildName);
		for (UseCaseScenarios scenarios : useCaseScenariosList
				.getUseCaseScenarios()) {
			calulateAggregatedDataForUseCase(branchName, buildName, scenarios);
		}
		
		// Calculate page variant counters
		HashMap<String, Integer> counters = new HashMap<String, Integer>();
		for (Entry<String, StepVariantState> entry : mapOfStepVariant
				.entrySet()) {
			StepVariantState variant = entry.getValue();
			counters.put(entry.getKey(), variant.getCounter());
			
			StepIdentification lastStep = variant.getPreviousStep();
			setNextVariant(branchName, buildName, null, lastStep,
					variant.getFirstStep());
			setPreviousVariant(branchName, buildName, variant.getFirstStep(),
					lastStep);
		}
		
		dao.savePageVariants(branchName, buildName, new PageVariantsCounter(counters));
		
		dao.saveUseCaseScenariosList(branchName, buildName, useCaseScenariosList);
		
		objectRepository.calculateAndSaveObjectLists();
		
		dao.saveLongObjectNamesIndex(branchName, buildName, longObjectNamesResolver);
		
		dao.saveVersion(branchName, buildName, CURRENT_FILE_FORMAT_VERSION);
		
	}
	
	private UseCaseScenariosList calculateUseCaseScenariosList(final String branchName, final String buildName) {
		
		UseCaseScenariosList result = new UseCaseScenariosList();
		List<UseCaseScenarios> useCaseScenarios = new ArrayList<UseCaseScenarios>();
		List<UseCase> usecases = reader.loadUsecases(branchName, buildName);
		for (UseCase usecase : usecases) {
			UseCaseScenarios item = new UseCaseScenarios();
			List<Scenario> scenarios = reader.loadScenarios(branchName,
					buildName, usecase.getName());
			boolean atLeastOneScenarioFailed = false;
			for (Scenario scenario : scenarios) {
				if (StringUtils.equals(scenario.getStatus(), "failed")) {
					atLeastOneScenarioFailed = true;
					break;
				}
			}
			usecase.setStatus(atLeastOneScenarioFailed ? "failed" : "success");
			item.setScenarios(scenarios);
			item.setUseCase(usecase);
			useCaseScenarios.add(item);
		}
		result.setUseCaseScenarios(useCaseScenarios);
		return result;
	}
	
	private void calulateAggregatedDataForUseCase(final String branchName,
			final String buildName, final UseCaseScenarios useCaseScenarios) {
		
		LOGGER.info("    calculating aggregated data for use case : " + useCaseScenarios.getUseCase().getName());
		
		List<ObjectReference> referencePath = objectRepository
				.createPath(objectRepository.createObjectReference("case", useCaseScenarios.getUseCase().getName()));
		objectRepository.addObjects(referencePath, useCaseScenarios.getUseCase().getDetails());
		
		for (Scenario scenario : useCaseScenarios.getScenarios()) {
			try {
				calculateAggregatedDataForScenario(referencePath, branchName, buildName, useCaseScenarios.getUseCase(),
						scenario);
			} catch (ResourceNotFoundException ex) {
				LOGGER.warn("could not load scenario " + scenario.getName()
						+ " in use case"
						+ useCaseScenarios.getUseCase().getName());
			}
		}
		dao.saveUseCaseScenarios(branchName, buildName, useCaseScenarios);
		
		objectRepository.updateAndSaveObjectIndexesForCurrentCase();
	}
	
	private void calculateAggregatedDataForScenario(List<ObjectReference> referencePath, final String branchName,
			final String buildName, final UseCase usecase,
			final Scenario scenario) {
		
		referencePath = objectRepository.addReferencedScenarioObjects(referencePath, scenario);
		
		LOGGER.info("      calculating aggregated data for scenario : "
				+ scenario.getName());
		ScenarioPageSteps scenarioPageSteps = calculateScenarioPageSteps(referencePath,
				branchName, buildName, usecase, scenario);
		
		dao.saveScenarioPageSteps(branchName, buildName, scenarioPageSteps);
	}
	
	private ScenarioPageSteps calculateScenarioPageSteps(final List<ObjectReference> referencePath,
			final String branchName, final String buildName,
			final UseCase usecase, final Scenario scenario) {
		
		ScenarioPageSteps result = new ScenarioPageSteps();
		result.setUseCase(usecase);
		result.setScenario(scenario);
		
		// pages and steps
		List<Step> steps = reader.loadSteps(branchName, buildName,
				usecase.getName(), scenario.getName());
		int numberOfSteps = steps.size();
		List<PageSteps> pageStepsList = new ArrayList<PageSteps>();
		Page page = null;
		PageSteps pageSteps = null;
		int pageIndex = 0;
		int pageStepIndex = 0;
		int index = 0;
		for (Step step : steps) {
			
			boolean isNewPage = page == null || step.getPage() == null
					|| !page.equals(step.getPage());
			if (isNewPage) {
				page = step.getPage();
				pageSteps = new PageSteps();
				pageSteps.setPage(page);
				pageSteps.setSteps(new ArrayList<StepDescription>());
				pageStepsList.add(pageSteps);
				pageStepIndex = 0;
				if (index > 0) {
					pageIndex++;
				}
			}
			StepDescription stepDescription = step.getStepDescription();
			stepDescription.setOccurence(pageIndex);
			stepDescription.setRelativeIndex(pageStepIndex);
			pageSteps.getSteps().add(stepDescription);
			
			StepIdentification stepIdentification = new StepIdentification(
					usecase.getName(), scenario.getName(), page.getName(),
					index, pageIndex, pageStepIndex);
			processStepVariant(branchName, buildName, pageStepsList, step,
					page.getName(), stepIdentification);
			
			objectRepository.addReferencedStepObjects(referencePath, step);
			
			index++;
			pageStepIndex++;
		}
		result.setPagesAndSteps(pageStepsList);
		
		// scenario calculated data from pages and steps
		ScenarioCalculatedData calculatedData = new ScenarioCalculatedData();
		calculatedData.setNumberOfPages(pageIndex);
		calculatedData.setNumberOfSteps(numberOfSteps);
		scenario.setCalculatedData(calculatedData);
		
		return result;
	}
	
	private void processStepVariant(final String branchName,
			final String buildName, final List<PageSteps> pageStepsList,
			final Step step, final String pageName,
			final StepIdentification stepIdentification) {
		StepVariantState variant = null;
		if (mapOfStepVariant.containsKey(pageName)) {
			variant = mapOfStepVariant.get(pageName);
		} else {
			variant = new StepVariantState(stepIdentification);
			mapOfStepVariant.put(pageName, variant);
		}
		
		variant.increaseCounter();
		if (variant.getPreviousStep() != null) {
			StepDescription stepDescription = step.getStepDescription();
			stepDescription.setVariantIndex(variant.getCounter());
			stepDescription.setPreviousStepVariant(variant.getPreviousStep());
			setNextVariant(branchName, buildName, pageStepsList,
					variant.getPreviousStep(), stepIdentification);
		}
		variant.setPreviousStep(stepIdentification);
	}
	
	private void setNextVariant(final String branchName,
			final String buildName, final List<PageSteps> pageStepsList,
			final StepIdentification step,
			final StepIdentification nextStepVariant) {
		if (pageStepsList != null && isSameScenario(nextStepVariant, step)) {
			PageSteps page = pageStepsList.get(step.getOccurence());
			StepDescription stepDescription = page.getSteps().get(
					step.getRelativeIndex());
			stepDescription.setNextStepVariant(nextStepVariant);
		} else {
			ScenarioPageSteps pageSteps = dao.loadScenarioPageSteps(branchName,
					buildName, step.getUseCaseName(), step.getScenarioName());
			PageSteps page = pageSteps.getPagesAndSteps().get(
					step.getOccurence());
			StepDescription stepDescription = page.getSteps().get(
					step.getRelativeIndex());
			stepDescription.setNextStepVariant(nextStepVariant);
			dao.saveScenarioPageSteps(branchName, buildName, pageSteps);
		}
	}
	
	private void setPreviousVariant(final String branchName, final String buildName, final StepIdentification step,
			final StepIdentification previousStepVariant) {
		ScenarioPageSteps pageSteps = dao.loadScenarioPageSteps(branchName,
				buildName, step.getUseCaseName(), step.getScenarioName());
		PageSteps page = pageSteps.getPagesAndSteps().get(step.getOccurence());
		StepDescription stepDescription = page.getSteps().get(
				step.getRelativeIndex());
		stepDescription.setPreviousStepVariant(previousStepVariant);
		dao.saveScenarioPageSteps(branchName, buildName, pageSteps);
	}
	
	private boolean isSameScenario(final StepIdentification stepIdentification, final StepIdentification nextStepVariant) {
		return stepIdentification.getUseCaseName().equals(nextStepVariant.getUseCaseName())
				&& stepIdentification.getScenarioName().equals(nextStepVariant.getScenarioName());
	}
	
	public void updateBuildSummary(final BuildImportSummary buildSummary, final BuildLink buildLink) {
		BuildIdentifier buildIdentifier = buildSummary.getIdentifier();
		buildSummary.setBuildDescription(buildLink.getBuild());
		String version = dao.loadVersion(buildIdentifier.getBranchName(), buildIdentifier.getBuildName());
		boolean aggregated = !StringUtils.isBlank(version);
		boolean outdated = aggregated && !version.equals(CURRENT_FILE_FORMAT_VERSION);
		boolean error = buildSummary.getStatus().isFailed();
		if (error) {
			buildSummary.setStatus(BuildImportStatus.FAILED);
		}
		else if (outdated) {
			buildSummary.setStatus(BuildImportStatus.OUTDATED);
		}
		else if (aggregated) {
			buildSummary.setStatus(BuildImportStatus.SUCCESS);
		}
		else {
			buildSummary.setStatus(BuildImportStatus.UNPROCESSED);
		}
	}
}
