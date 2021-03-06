/* scenarioo-api
 * Copyright (C) 2014, scenarioo.org Development Team
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * As a special exception, the copyright holders of this library give you 
 * permission to link this library with independent modules, according 
 * to the GNU General Public License with "Classpath" exception as provided
 * in the LICENSE file that accompanied this code.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.scenarioo.api;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Date;


import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.scenarioo.api.ScenarioDocuReader;
import org.scenarioo.api.ScenarioDocuWriter;
import org.scenarioo.api.files.ScenarioDocuFiles;
import org.scenarioo.model.docu.entities.Branch;
import org.scenarioo.model.docu.entities.Build;
import org.scenarioo.model.docu.entities.Page;
import org.scenarioo.model.docu.entities.Scenario;
import org.scenarioo.model.docu.entities.Step;
import org.scenarioo.model.docu.entities.StepDescription;
import org.scenarioo.model.docu.entities.StepHtml;
import org.scenarioo.model.docu.entities.StepMetadata;
import org.scenarioo.model.docu.entities.UseCase;
import org.scenarioo.model.docu.entities.generic.Details;
import org.scenarioo.model.docu.entities.generic.ObjectDescription;
import org.scenarioo.model.docu.entities.generic.ObjectList;
import org.scenarioo.model.docu.entities.generic.ObjectReference;

/**
 * Some smoke tests for the Scenarioo generator API.
 */
public class ScenarioDocuWriterAndReaderTest {
	
	private static final File TEST_ROOT_DIRECTORY = new File("tmpScenarioDocuUnitTestFiles");
	private static final String TEST_BRANCH_NAME = "testBranch";
	private static final String TEST_BUILD_NAME = "testBuild";
	private static final String TEST_CASE_NAME = "testCase";
	private static final String TEST_SCENARIO_NAME = "testScenario";
	private static int TEST_STEP_INDEX = 1;
	
	private static final String TEST_DETAILS_VERSION_KEY = "version";
	
	/**
	 * Objects under test
	 */
	private ScenarioDocuWriter writer;
	private ScenarioDocuReader reader;
	
	/**
	 * docuFiles is used for checking some of the written files.
	 */
	private ScenarioDocuFiles docuFiles;
	
	@Before
	public void setUp() {
		TEST_ROOT_DIRECTORY.mkdirs();
		writer = new ScenarioDocuWriter(TEST_ROOT_DIRECTORY, TEST_BRANCH_NAME, TEST_BUILD_NAME);
		reader = new ScenarioDocuReader(TEST_ROOT_DIRECTORY);
		docuFiles = new ScenarioDocuFiles(TEST_ROOT_DIRECTORY);
	}
	
	@AfterClass
	public static void tearDown() {
		deleteDirectory(TEST_ROOT_DIRECTORY);
	}
	
	private static void deleteDirectory(final File testRootDirectory) {
		try {
			FileUtils.deleteDirectory(TEST_ROOT_DIRECTORY);
		} catch (IOException e) {
			throw new RuntimeException("Could not delete test data directory", e);
		}
	}
	
	@Test
	public void write_and_read_branch_description() {
		
		// GIVEN: a typical branch
		Branch branch = new Branch();
		branch.setName(TEST_BRANCH_NAME);
		branch.setDescription("just a simple development branch, might as well be the trunk.");
		
		// WHEN: the branch was saved.
		writer.saveBranchDescription(branch);
		writer.flush();
		
		// THEN: the branch can be loaded successfully and correctly
		Branch branchFromFile = reader.loadBranch(TEST_BRANCH_NAME);
		assertEquals("expected branch name", TEST_BRANCH_NAME, branchFromFile.getName());
		assertEquals("expected branch description", branch.getDescription(), branchFromFile.getDescription());
		
	}
	
	@Test
	public void write_and_read_build_description() {
		
		// GIVEN: a typical build
		Build build = new Build();
		build.setName(TEST_BUILD_NAME);
		build.setDate(new Date());
		build.setRevision("10123");
		build.setStatus("success");
		build.getDetails().addDetail(TEST_DETAILS_VERSION_KEY, "1.0.1");
		
		// WHEN: the build was saved.
		writer.saveBuildDescription(build);
		writer.flush();
		
		// THEN: the build can be loaded successfully and correctly
		Build buildFromFile = reader.loadBuild(TEST_BRANCH_NAME, TEST_BUILD_NAME);
		assertEquals("expected build name", TEST_BUILD_NAME, buildFromFile.getName());
		assertEquals("expected date", build.getDate(), buildFromFile.getDate());
		assertEquals("expected revision", build.getRevision(), buildFromFile.getRevision());
		assertEquals("expected state", build.getStatus(), buildFromFile.getStatus());
		assertEquals("expected details properties", build.getDetails().getProperties(), buildFromFile.getDetails()
				.getProperties());
		
	}
	
	@Test
	public void write_and_read_case_description() {
		
		// GIVEN: a typical use case
		UseCase usecase = new UseCase();
		usecase.setName(TEST_CASE_NAME);
		usecase.setDescription("this is a typical use case with a decription");
		usecase.setStatus("success");
		usecase.getDetails().addDetail("webtestName", "UseCaseWebTest");
		
		// WHEN: the usecase was saved.
		writer.saveUseCase(usecase);
		writer.flush();
		
		// THEN: the usecase can be loaded successfully and correctly
		UseCase usecaseFromFile = reader.loadUsecase(TEST_BRANCH_NAME, TEST_BUILD_NAME, TEST_CASE_NAME);
		assertEquals("expected case name", TEST_CASE_NAME, usecaseFromFile.getName());
		assertEquals("expected case desription", usecase.getDescription(), usecaseFromFile.getDescription());
		assertEquals("expected case state", usecase.getStatus(), usecaseFromFile.getStatus());
		assertEquals("expected details properties", usecase.getDetails().getProperties(), usecaseFromFile.getDetails()
				.getProperties());
		
	}
	
	@Test
	public void write_and_read_scenario_description() {
		
		// GIVEN: a typical scenario
		Scenario scenario = new Scenario();
		scenario.setName(TEST_SCENARIO_NAME);
		scenario.setDescription("this is a typical scenario with a decription");
		scenario.setStatus("success");
		scenario.getDetails().addDetail("userRole", "customer");
		
		// WHEN: the scenario was saved.
		writer.saveScenario(TEST_CASE_NAME, scenario);
		writer.flush();
		
		// THEN: the scenario can be loaded successfully and correctly
		Scenario scenarioFromFile = reader.loadScenario(TEST_BRANCH_NAME, TEST_BUILD_NAME, TEST_CASE_NAME,
				TEST_SCENARIO_NAME);
		assertEquals("expected scneario name", TEST_SCENARIO_NAME, scenarioFromFile.getName());
		assertEquals("expected scenario desription", scenario.getDescription(), scenarioFromFile.getDescription());
		assertEquals("expected scenario state", scenario.getStatus(), scenarioFromFile.getStatus());
		assertEquals("expected scenario details properties", scenario.getDetails().getProperties(),
				scenarioFromFile.getDetails().getProperties());
		
	}
	
	@Test
	public void write_and_read_step() {
		
		// GIVEN: a typical step
		Step step = new Step();
		StepDescription stepDescription = new StepDescription();
		stepDescription.setIndex(TEST_STEP_INDEX);
		stepDescription.setTitle("Test Step");
		stepDescription.setStatus("success");
		step.setStepDescription(stepDescription);
		step.setHtml(new StepHtml("<html>just some page text</html>"));
		step.setPage(new Page("customer/overview.jsp"));
		StepMetadata stepMetadata = new StepMetadata();
		stepMetadata.setVisibleText("just some page text");
		stepMetadata.getDetails().addDetail("mockedServicesConfiguration", "dummy_config_xy.properties");
		step.setMetadata(stepMetadata);
		
		// WHEN: the step was saved.
		writer.saveStep(TEST_CASE_NAME, TEST_SCENARIO_NAME, step);
		writer.flush();
		
		// THEN: the step can be loaded successfully and correctly
		Step stepFromFile = reader.loadStep(TEST_BRANCH_NAME, TEST_BUILD_NAME, TEST_CASE_NAME,
				TEST_SCENARIO_NAME, TEST_STEP_INDEX);
		assertEquals("expected step name", TEST_STEP_INDEX, stepFromFile.getStepDescription().getIndex());
		assertEquals("expected step desription", step.getStepDescription().getTitle(), stepFromFile
				.getStepDescription().getTitle());
		assertEquals("expected step state", step.getStepDescription().getStatus(), stepFromFile.getStepDescription()
				.getStatus());
		assertEquals("expected step html", step.getHtml().getHtmlSource(), stepFromFile.getHtml().getHtmlSource());
		assertEquals("expected step page name", step.getPage().getName(), stepFromFile.getPage().getName());
		assertEquals("expected step metadata details properties", step.getMetadata().getDetails().getProperties(),
				stepFromFile.getMetadata().getDetails().getProperties());
		
	}
	
	/**
	 * Tests writing and reading of a scenario docu file containing some basic collections that need to be supported.
	 */
	@Test
	public void write_and_read_generic_collections_in_details() {
		
		// GIVEN: any object containing collections in details
		Scenario scenario = new Scenario();
		scenario.setName(TEST_SCENARIO_NAME);
		scenario.setDescription("a scenario for testing collections in details");
		// List of Strings
		ObjectList<String> list = new ObjectList<String>();
		list.add("item1");
		list.add("item2");
		list.add("item3");
		scenario.getDetails().addDetail("list", list);
		// Details (further maps with key value pairs for structured objects)
		Details detailsMap = new Details();
		detailsMap.put("key1", "value1");
		detailsMap.put("key2", "value2");
		detailsMap.put("anyGenericObjectReference", new ObjectReference("serviceCall", "MainDB.getUsers"));
		detailsMap.put("anyGenericObject", new ObjectDescription("configuration",
				"my_dummy_mocks_configuration.properties"));
		scenario.getDetails().addDetail("map", detailsMap);
		
		// WHEN: the object was saved.
		writer.saveScenario(TEST_CASE_NAME, scenario);
		writer.flush();
		
		// THEN: the collections get loaded correctly again.
		Scenario scenarioFromFile = reader.loadScenario(TEST_BRANCH_NAME, TEST_BUILD_NAME, TEST_CASE_NAME,
				TEST_SCENARIO_NAME);
		assertEquals(list, scenarioFromFile.getDetails().getDetail("list"));
		assertEquals(detailsMap, scenarioFromFile.getDetails().getDetail("map"));
		assertEquals(scenario.getDetails(), scenarioFromFile.getDetails());
		
	}
	
	/**
	 * Test that the files are written asynchronously and that the flush method waits correctly for the last file to be
	 * written.
	 */
	@Test
	public void async_write_of_multiple_files_and_flush() {
		
		// GIVEN: a lot of large steps to write, that have not yet been written
		Step[] steps = new Step[10];
		for (int index = 0; index < 10; index++) {
			steps[index] = createBigDataStepForLoadTestAsyncWriting(index + 1);
		}
		File expectedFileForStep10 = docuFiles.getStepFile(TEST_BRANCH_NAME, TEST_BUILD_NAME, TEST_CASE_NAME,
				TEST_SCENARIO_NAME, 10);
		assertFalse(expectedFileForStep10.exists());
		
		// WHEN: saving those steps,
		for (int index = 0; index < 10; index++) {
			writer.saveStep(TEST_CASE_NAME, TEST_SCENARIO_NAME, steps[index]);
		}
		
		// THEN: the files are not created directly but asynchronously. flush will wait until all save's are finished.
		assertFalse(expectedFileForStep10.exists());
		writer.flush();
		assertTrue(expectedFileForStep10.exists());
		
	}
	
	private Step createBigDataStepForLoadTestAsyncWriting(final int index) {
		
		Step step = new Step();
		
		// Description
		StepDescription stepDescription = new StepDescription();
		stepDescription.setIndex(index);
		stepDescription.setScreenshotFileName(docuFiles.getScreenshotFile(TEST_BRANCH_NAME, TEST_BUILD_NAME,
				TEST_CASE_NAME, TEST_SCENARIO_NAME, index).getName());
		stepDescription
				.setTitle("this is a step with a lot of data in it such that writing should take realy long for testing async writing");
		step.setStepDescription(stepDescription);
		
		// Metdata with a lot of details
		step.setMetadata(createBigMetadata());
		
		// Page
		step.setPage(new Page("test.jsp"));
		
		// HTML (lot of dummy data, just to generate big data for writing)
		step.setHtml(createBigHtml());
		
		return step;
	}
	
	/**
	 * Simply generate StepMetadata object with a lot of details to get a big step for load testing of writing.
	 */
	private StepMetadata createBigMetadata() {
		StepMetadata stepMetadata = new StepMetadata();
		for (int i = 0; i < 1000; i++) {
			stepMetadata.getDetails().addDetail("detail" + i,
					"just a detail to produce a lot of data that needs marshalling and writing.");
		}
		return stepMetadata;
	}
	
	/**
	 * Simply generate a big dummy html for load testing of writing.
	 */
	private StepHtml createBigHtml() {
		
		StringBuilder builder = new StringBuilder();
		builder.append("<html><head></head><body>");
		builder.append("<p>This is just a dummy html code with lot of content to generate a lot of big data to write for load testing.<p>");
		for (int i = 0; i < 1000; i++) {
			builder.append("<div class=\"dummyParagraph"
					+ i
					+ "\">This is just a dummy html code with lot of content to generate a lot of big data to write for load testing.</div>");
		}
		builder.append("</body></html>");
		
		StepHtml html = new StepHtml();
		html.setHtmlSource(builder.toString());
		return html;
		
	}
	
}
