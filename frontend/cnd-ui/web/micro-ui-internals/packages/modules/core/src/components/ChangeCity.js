import { Dropdown } from "@nudmcdgnpm/digit-ui-react-components";
import React, { useState, useEffect } from "react";
import { CustomButton, Menu } from "@nudmcdgnpm/digit-ui-react-components";
import { useHistory } from "react-router-dom";

const stringReplaceAll = (str = "", searcher = "", replaceWith = "") => {
  if (searcher == "") return str;
  while (str?.includes(searcher)) {
    str = str?.replace(searcher, replaceWith);
  }
  return str;
};

const ChangeCity = (prop) => {
  const [dropDownData, setDropDownData] = useState(null);
  const [selectCityData, setSelectCityData] = useState([]);
  const [selectedCity, setSelectedCity] = useState([]); //selectedCities?.[0]?.value
  const history = useHistory();
  const isDropdown = prop.dropdown || false;
  let selectedCities = [];

  const handleChangeCity = (city) => {
    const loggedInData = Digit.SessionStorage.get("citizen.userRequestObject");
    const filteredRoles = Digit.SessionStorage.get("citizen.userRequestObject")?.info?.roles?.filter(role => role.tenantId === city.value);
    if (filteredRoles?.length > 0) {
      loggedInData.info.roles = filteredRoles;
      loggedInData.info.tenantId = city?.value;
    }
    Digit.SessionStorage.set("Employee.tenantId", city?.value);
    Digit.UserService.setUser(loggedInData);
    setDropDownData(city);
    if (window.location.href.includes("/cnd-ui/employee/")) {
      const redirectPath = location.state?.from || "/cnd-ui/employee";
      history.replace(redirectPath);
    }
    window.location.reload();
  };

  useEffect(() => {
    const userloggedValues = Digit.SessionStorage.get("citizen.userRequestObject");
    let teantsArray = [], filteredArray = [];
    userloggedValues?.info?.roles?.forEach(role => teantsArray.push(role.tenantId));
    let unique = teantsArray.filter((item, i, ar) => ar.indexOf(item) === i);
    unique?.forEach(uniCode => {
      filteredArray.push({
        label: prop?.t(`TENANT_TENANTS_${stringReplaceAll(uniCode, ".", "_")?.toUpperCase()}`),
        value: uniCode
      })
    });
    selectedCities = filteredArray?.filter(select => select.value == Digit.SessionStorage.get("Employee.tenantId"));
    setSelectCityData(filteredArray);
  }, [dropDownData]);

  // if (isDropdown) {
  return (
    <div style={prop?.mobileView ? {color: "#767676"} : {}}>
      <Dropdown
        option={selectCityData}
        selected={selectCityData.find((cityValue) => cityValue.value === dropDownData?.value)}
        optionKey={"label"}
        select={handleChangeCity}
        freeze={true}
        customSelector={<label className="cp">{prop?.t(`TENANT_TENANTS_${stringReplaceAll(Digit.SessionStorage.get("Employee.tenantId"), ".", "_")?.toUpperCase()}`)}</label>}
      />
    </div>
  );
};

export default ChangeCity;
