/*
 * eGov  SmartCity eGovernance suite aims to improve the internal efficiency,transparency,


 * accountability and the service delivery of the government  organizations.
 *
 *  Copyright (C) <2019>  eGovernments Foundation
 *
 *  The updated version of eGov suite of products as by eGovernments Foundation
 *  is available at http://www.egovernments.org
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see http://www.gnu.org/licenses/ or
 *  http://www.gnu.org/licenses/gpl.html .
 *
 *  In addition to the terms of the GPL license to be adhered to in using this
 *  program, the following additional terms are to be complied with:
 *
 *      1) All versions of this program, verbatim or modified must carry this
 *         Legal Notice.
 *      Further, all user interfaces, including but not limited to citizen facing interfaces,
 *         Urban Local Bodies interfaces, dashboards, mobile applications, of the program and any
 *         derived works should carry eGovernments Foundation logo on the top right corner.
 *
 *      For the logo, please refer http://egovernments.org/html/logo/egov_logo.png.
 *      For any further queries on attribution, including queries on brand guidelines,
 *         please contact contact@egovernments.org
 *
 *      2) Any misrepresentation of the origin of the material is prohibited. It
 *         is required that all modified versions of this material be marked in
 *         reasonable ways as different from the original version.
 *
 *      3) This license does not grant any rights to any user of the program
 *         with regards to rights under trademark law for use of the trade names
 *         or trademarks of eGovernments Foundation.
 *
 *  In case of any queries, you can reach eGovernments Foundation at contact@egovernments.org.
 */

package org.egov.edcr.feature;

import static org.egov.edcr.constants.DxfFileConstants.B;
import static org.egov.edcr.constants.DxfFileConstants.D;
import static org.egov.edcr.constants.DxfFileConstants.F;
import static org.egov.edcr.constants.DxfFileConstants.F_CB;
import static org.egov.edcr.constants.DxfFileConstants.F_RT;
import static org.egov.edcr.constants.DxfFileConstants.G;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.egov.common.constants.MdmsFeatureConstants;
import org.egov.common.entity.dcr.helper.OccupancyHelperDetail;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.constants.EdcrRulesMdmsConstants;
import org.egov.edcr.service.FetchEdcrRulesMdms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RoadWidth_Citya extends FeatureProcess {

    private static final Logger LOG = LogManager.getLogger(RoadWidth_Citya.class);
    private static final String RULE_34 = "34-1";
    public static final String ROADWIDTH_DESCRIPTION = "Minimum Road Width";
    public static final String NEW = "NEW";
    
    @Autowired
    FetchEdcrRulesMdms fetchEdcrRulesMdms;

    @Override
    public Map<String, Date> getAmendments() {
        return new LinkedHashMap<>(); // No amendments defined for this feature
    }

    @Override
    public Plan validate(Plan pl) {
        return pl; // No validation logic currently implemented
    }

    @Override
    public Plan process(Plan pl) {
        // Check if road width is provided in plan information
        if (pl.getPlanInformation() != null && pl.getPlanInformation().getRoadWidth() != null) {
            BigDecimal roadWidth = pl.getPlanInformation().getRoadWidth();
            String typeOfArea = pl.getPlanInformation().getTypeOfArea();

            // Apply rule only for NEW area types
            if (typeOfArea != null && NEW.equalsIgnoreCase(typeOfArea)) {
                ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
                scrutinyDetail.setKey("Common_Road Width"); // Setting the scrutiny detail key
                scrutinyDetail.addColumnHeading(1, RULE_NO);
                scrutinyDetail.addColumnHeading(2, DESCRIPTION);
                scrutinyDetail.addColumnHeading(3, OCCUPANCY);
                scrutinyDetail.addColumnHeading(4, PERMITTED);
                scrutinyDetail.addColumnHeading(5, PROVIDED);
                scrutinyDetail.addColumnHeading(6, STATUS);

                Map<String, String> details = new HashMap<>();
                details.put(RULE_NO, RULE_34);
                details.put(DESCRIPTION, ROADWIDTH_DESCRIPTION);

                // Get permissible road width values for the occupancy
                Map<String, BigDecimal> occupancyValuesMap = getOccupancyValues(pl);

                // Fetch occupancy type from the most restrictive FAR helper
                if (pl.getVirtualBuilding() != null && pl.getVirtualBuilding().getMostRestrictiveFarHelper() != null) {
                    OccupancyHelperDetail occupancyType = pl.getVirtualBuilding().getMostRestrictiveFarHelper()
                            .getSubtype() != null
                                    ? pl.getVirtualBuilding().getMostRestrictiveFarHelper().getSubtype()
                                    : pl.getVirtualBuilding().getMostRestrictiveFarHelper().getType();

                    if (occupancyType != null) {
                        details.put(OCCUPANCY, occupancyType.getName());

                        // Fetch the required road width for this occupancy type
                        BigDecimal roadWidthRequired = occupancyValuesMap.get(occupancyType.getCode());
                        if (roadWidthRequired != null) {
                            // Check if provided road width is acceptable
                            if (roadWidth.compareTo(roadWidthRequired) >= 0) {
                                details.put(PERMITTED, String.valueOf(roadWidthRequired) + "m");
                                details.put(PROVIDED, roadWidth.toString() + "m");
                                details.put(STATUS, Result.Accepted.getResultVal());
                                scrutinyDetail.getDetail().add(details);
                                pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
                            } else {
                                details.put(PERMITTED, String.valueOf(roadWidthRequired) + "m");
                                details.put(PROVIDED, roadWidth.toString() + "m");
                                details.put(STATUS, Result.Not_Accepted.getResultVal());
                                scrutinyDetail.getDetail().add(details);
                                pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
                            }
                        }
                    }
                }
            }
        }
        return pl;
    }

    // Method to retrieve permissible road width values from MDMS rules
    public Map<String, BigDecimal> getOccupancyValues(Plan plan) {
    	
    	BigDecimal roadWidthValue = BigDecimal.ZERO; // Default road width value
    	
        String occupancyName = null;
		
		String feature = MdmsFeatureConstants.ROAD_WIDTH;

		// Determine occupancy type from the FAR helper
		Map<String, Object> params = new HashMap<>();
		if(DxfFileConstants.A.equals(plan.getVirtualBuilding().getMostRestrictiveFarHelper().getType().getCode())){
			occupancyName = "Residential";
		}

		params.put("feature", feature);
		params.put("occupancy", occupancyName);

		Map<String,List<Map<String,Object>>> edcrRuleList = plan.getEdcrRulesFeatures();

		ArrayList<String> valueFromColumn = new ArrayList<>();
		valueFromColumn.add(EdcrRulesMdmsConstants.PERMISSIBLE_VALUE);

		List<Map<String, Object>> permissibleValue = new ArrayList<>();

		// Fetch permissible value using MDMS service
		permissibleValue = fetchEdcrRulesMdms.getPermissibleValue(edcrRuleList, params, valueFromColumn);
		LOG.info("permissibleValue" + permissibleValue);

		// Extract the road width value if present
		if (!permissibleValue.isEmpty() && permissibleValue.get(0).containsKey(EdcrRulesMdmsConstants.PERMISSIBLE_VALUE)) {
			roadWidthValue = BigDecimal.valueOf(Double.valueOf(permissibleValue.get(0).get(EdcrRulesMdmsConstants.PERMISSIBLE_VALUE).toString()));
		}

        // Assign retrieved value to all relevant occupancy codes
        Map<String, BigDecimal> roadWidthValues = new HashMap<>();
        roadWidthValues.put(B, roadWidthValue);
        roadWidthValues.put(D, roadWidthValue);
        roadWidthValues.put(G, roadWidthValue);
        roadWidthValues.put(F, roadWidthValue);
        roadWidthValues.put(F_RT, roadWidthValue);
        roadWidthValues.put(F_CB, roadWidthValue);
        return roadWidthValues;

    }
}

