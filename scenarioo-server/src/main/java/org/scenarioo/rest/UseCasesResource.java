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

package org.scenarioo.rest;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.apache.log4j.Logger;
import org.scenarioo.business.builds.ScenarioDocuBuildsManager;
import org.scenarioo.dao.aggregates.ScenarioDocuAggregationDAO;
import org.scenarioo.dao.configuration.ConfigurationDAO;
import org.scenarioo.model.docu.aggregates.usecases.UseCaseScenarios;

@Path("/rest/branches/{branchName}/builds/{buildName}/usecases/")
public class UseCasesResource {
	
	private static final Logger LOGGER = Logger.getLogger(UseCasesResource.class);
	
	ScenarioDocuAggregationDAO dao = new ScenarioDocuAggregationDAO(ConfigurationDAO.getDocuDataDirectoryPath());
	
	@GET
	@Produces({ "application/xml", "application/json" })
	public List<UseCaseScenarios> loadUseCaseScenariosList(@PathParam("branchName") final String branchName,
			@PathParam("buildName") final String buildName) {
		LOGGER.info("REQUEST: loadUseCaseScenariosList(" + branchName + ", " + buildName + ")");
		String resolvedBuildName = ScenarioDocuBuildsManager.INSTANCE.resolveAliasBuildName(branchName, buildName);
		return dao.loadUseCaseScenariosList(branchName, resolvedBuildName);
	}
}
