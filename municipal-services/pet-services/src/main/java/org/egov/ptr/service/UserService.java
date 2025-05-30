package org.egov.ptr.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.Role;
import org.egov.common.contract.request.User;
import org.egov.ptr.config.PetConfiguration;
import org.egov.ptr.models.PetRegistrationApplication;
import org.egov.ptr.models.PetRegistrationRequest;
import org.egov.ptr.models.user.CreateUserRequest;
import org.egov.ptr.models.user.UserDetailResponse;
import org.egov.ptr.models.user.UserSearchRequest;
import org.egov.ptr.repository.ServiceRequestRepository;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class UserService {

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private ServiceRequestRepository serviceRequestRepository;

	@Autowired
	private PetConfiguration petConfiguration;

	/**
	 * Creates user if it is not created already
	 * 
	 * @param request PetRegistrationRequest received for creating application
	 */
	public void createUser(PetRegistrationRequest request) {

		PetRegistrationApplication petApplication = request.getPetRegistrationApplications().get(0);
		RequestInfo requestInfo = request.getRequestInfo();
		Role role = getCitizenRole();
		User owner = new User();
		addUserDefaultFields(petApplication.getTenantId(), role, owner);
		UserDetailResponse userDetailResponse = userExists(owner, requestInfo);
		List<User> existingUsersFromService = userDetailResponse.getUser();
		Map<String, User> ownerMapFromSearch = existingUsersFromService.stream()
				.collect(Collectors.toMap(User::getUuid, Function.identity()));

		if (CollectionUtils.isEmpty(existingUsersFromService)) {

			owner.setUserName(UUID.randomUUID().toString());
			userDetailResponse = createUser(requestInfo, owner);

		} else {

			String uuid = owner.getUuid();
			if (uuid != null && ownerMapFromSearch.containsKey(uuid)) {
				userDetailResponse = updateExistingUser(petApplication, requestInfo, role, owner,
						ownerMapFromSearch.get(uuid));
			} else {

				owner.setUserName(UUID.randomUUID().toString());
				userDetailResponse = createUser(requestInfo, owner);
			}
		}
		setOwnerFields(owner, userDetailResponse, requestInfo);
	}

	/**
	 * update existing user
	 * 
	 */
	private UserDetailResponse updateExistingUser(PetRegistrationApplication petApplication, RequestInfo requestInfo,
			Role role, User ownerFromRequest, User ownerInfoFromSearch) {

		UserDetailResponse userDetailResponse;

		ownerFromRequest.setId(ownerInfoFromSearch.getId());
		ownerFromRequest.setUuid(ownerInfoFromSearch.getUuid());
		addUserDefaultFields(petApplication.getTenantId(), role, ownerFromRequest);

		StringBuilder uri = new StringBuilder(petConfiguration.getUserHost())
				.append(petConfiguration.getUserContextPath()).append(petConfiguration.getUserSearchEndpoint());
		userDetailResponse = userCall(
				CreateUserRequest.builder().requestInfo(requestInfo).user(ownerFromRequest).build(), uri);
		if (userDetailResponse.getUser().get(0).getUuid() == null) {
			throw new CustomException("INVALID USER RESPONSE", "The user updated has uuid as null");
		}
		return userDetailResponse;
	}

	private UserDetailResponse createUser(RequestInfo requestInfo, User owner) {
		UserDetailResponse userDetailResponse;
		StringBuilder uri = new StringBuilder(petConfiguration.getUserHost())
				.append(petConfiguration.getUserContextPath()).append(petConfiguration.getUserSearchEndpoint());

		CreateUserRequest userRequest = CreateUserRequest.builder().requestInfo(requestInfo).user(owner).build();

		userDetailResponse = userCall(userRequest, uri);

		if (ObjectUtils.isEmpty(userDetailResponse)) {

			throw new CustomException("INVALID USER RESPONSE",
					"The user create has failed for the mobileNumber : " + owner.getUserName());

		}
		return userDetailResponse;
	}

	/**
	 * Sets the role,type,active and tenantId for a Citizen
	 * 
	 * @param tenantId TenantId of the pet application
	 * @param role     The role of the user set in this case to CITIZEN
	 * @param owner    The user whose fields are to be set
	 */
	private void addUserDefaultFields(String tenantId, Role role, User owner) {

		owner.setTenantId(tenantId);
		owner.setRoles(Collections.singletonList(role));
		owner.setType("CITIZEN");
	}

	private Role getCitizenRole() {

		return Role.builder().code("CITIZEN").name("Citizen").build();
	}

	/**
	 * Searches if the owner is already created. Search is based on name of owner,
	 * uuid and mobileNumber
	 * 
	 * @param owner       Owner which is to be searched
	 * @param requestInfo RequestInfo from the PetRegistrationRequest
	 * @return UserDetailResponse containing the user if present and the
	 *         responseInfo
	 */
	private UserDetailResponse userExists(User owner, RequestInfo requestInfo) {

		UserSearchRequest userSearchRequest = getBaseUserSearchRequest(owner.getTenantId(), requestInfo);
		userSearchRequest.setMobileNumber(owner.getMobileNumber());
		userSearchRequest.setUserType(owner.getType());
		userSearchRequest.setName(owner.getName());

		StringBuilder uri = new StringBuilder(petConfiguration.getUserHost())
				.append(petConfiguration.getUserSearchEndpoint());
		return userCall(userSearchRequest, uri);
	}

	/**
	 * Sets userName for the owner as mobileNumber if mobileNumber already assigned
	 * last 10 digits of currentTime is assigned as userName
	 * 
	 * @param owner              owner whose username has to be assigned
	 * @param listOfMobileNumber list of unique mobileNumbers in the
	 *                           PetRegistrationRequest
	 */
	private void setUserName(User owner, Set<String> listOfMobileNumber) {

		if (listOfMobileNumber.contains(owner.getMobileNumber())) {
			owner.setUserName(owner.getMobileNumber());
			// Once mobileNumber is set as userName it is removed from the list
			listOfMobileNumber.remove(owner.getMobileNumber());
		} else {
			String username = UUID.randomUUID().toString();
			owner.setUserName(username);
		}
	}

	/**
	 * Returns user using user search based on petApplicationCriteria(owner
	 * name,mobileNumber,userName)
	 * 
	 * @param userSearchRequest
	 * @return serDetailResponse containing the user if present and the responseInfo
	 */
	public UserDetailResponse getUser(UserSearchRequest userSearchRequest) {

		StringBuilder uri = new StringBuilder(petConfiguration.getUserHost())
				.append(petConfiguration.getUserSearchEndpoint());
		UserDetailResponse userDetailResponse = userCall(userSearchRequest, uri);
		return userDetailResponse;
	}

	/**
	 * Returns UserDetailResponse by calling user service with given uri and object
	 * 
	 * @param userRequest Request object for user service
	 * @param url         The address of the endpoint
	 * @return Response from user service as parsed as userDetailResponse
	 */
	@SuppressWarnings("unchecked")
	private UserDetailResponse userCall(Object userRequest, StringBuilder url) {

		String dobFormat = determineDobFormat(url.toString());
		try {
			Optional<Object> response = serviceRequestRepository.fetchResult(url, userRequest);

			if (response.isPresent()) {
				LinkedHashMap<String, Object> responseMap = (LinkedHashMap<String, Object>) response.get();
				parseResponse(responseMap, dobFormat);
				UserDetailResponse userDetailResponse = mapper.convertValue(responseMap, UserDetailResponse.class);
				return userDetailResponse;
			} else {
				return new UserDetailResponse();
			}
		}
		// Which Exception to throw?
		catch (IllegalArgumentException e) {
			throw new CustomException("IllegalArgumentException", "ObjectMapper not able to convertValue in userCall");
		}
	}

	/**
	 * Determines the date format based on the URL endpoint
	 * 
	 * @param url The URL of the endpoint
	 * @return The appropriate date format
	 */
	private String determineDobFormat(String url) {
		if (url.contains(petConfiguration.getUserSearchEndpoint()) || url.contains(petConfiguration.getUserSearchEndpoint())) {
			return "yyyy-MM-dd";
		} else if (url.contains(petConfiguration.getUserSearchEndpoint())) {
			return "dd/MM/yyyy";
		}
		return null;
	}
	
	/**
	 * Parses date formats to long for all users in responseMap
	 * 
	 * @param responeMap LinkedHashMap got from user api response
	 * @param dobFormat  dob format (required because dob is returned in different
	 *                   format's in search and create response in user service)
	 */
	@SuppressWarnings("unchecked")
	private void parseResponse(LinkedHashMap<String, Object> responeMap, String dobFormat) {

		List<LinkedHashMap<String, Object>> users = (List<LinkedHashMap<String, Object>>) responeMap.get("user");
		String format1 = "dd-MM-yyyy HH:mm:ss";

		if (null != users) {

			users.forEach(map -> {

				map.put("createdDate", dateTolong((String) map.get("createdDate"), format1));
				if ((String) map.get("lastModifiedDate") != null)
					map.put("lastModifiedDate", dateTolong((String) map.get("lastModifiedDate"), format1));
				if ((String) map.get("dob") != null)
					map.put("dob", dateTolong((String) map.get("dob"), dobFormat));
				if ((String) map.get("pwdExpiryDate") != null)
					map.put("pwdExpiryDate", dateTolong((String) map.get("pwdExpiryDate"), format1));
			});
		}
	}

	/**
	 * Converts date to long
	 * 
	 * @param date   date to be parsed
	 * @param format Format of the date
	 * @return Long value of date
	 */
	private Long dateTolong(String date, String format) {
		SimpleDateFormat f = new SimpleDateFormat(format);
		Date d = null;
		try {
			d = f.parse(date);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return d.getTime();
	}

	/**
	 * Sets owner fields (so that the owner table can be linked to user table)
	 * 
	 * @param owner              Owner in the pet detail whose user is created
	 * @param userDetailResponse userDetailResponse from the user Service
	 *                           corresponding to the given owner
	 */
	private void setOwnerFields(User owner, UserDetailResponse userDetailResponse, RequestInfo requestInfo) {

		owner.setUuid(userDetailResponse.getUser().get(0).getUuid());
		owner.setId(userDetailResponse.getUser().get(0).getId());
		owner.setUserName((userDetailResponse.getUser().get(0).getUserName()));
	}

	/**
	 * Updates user if present else creates new user
	 * 
	 * @param request PetRequest received from update
	 */

	/**
	 * provides a user search request with basic mandatory parameters
	 * 
	 * @param tenantId
	 * @param requestInfo
	 * @return
	 */
	public UserSearchRequest getBaseUserSearchRequest(String tenantId, RequestInfo requestInfo) {

		return UserSearchRequest.builder().requestInfo(requestInfo).userType("CITIZEN").tenantId(tenantId).active(true)
				.build();
	}

	public UserDetailResponse searchByUserName(String userName, String tenantId) {
		UserSearchRequest userSearchRequest = new UserSearchRequest();
		userSearchRequest.setUserType("CITIZEN");
		userSearchRequest.setUserName(userName);
		userSearchRequest.setTenantId(tenantId);
		return getUser(userSearchRequest);
	}

	private String getStateLevelTenant(String tenantId) {
		return tenantId.split("\\.")[0];
	}

	private UserDetailResponse searchedSingleUserExists(User owner, RequestInfo requestInfo) {

		UserSearchRequest userSearchRequest = getBaseUserSearchRequest(owner.getTenantId(), requestInfo);
		userSearchRequest.setUserType(owner.getType());
		Set<String> uuids = new HashSet<String>();
		uuids.add(owner.getUuid());
		userSearchRequest.setUuid(uuids);

		StringBuilder uri = new StringBuilder(petConfiguration.getUserHost())
				.append(petConfiguration.getUserSearchEndpoint());
		return userCall(userSearchRequest, uri);
	}

}
