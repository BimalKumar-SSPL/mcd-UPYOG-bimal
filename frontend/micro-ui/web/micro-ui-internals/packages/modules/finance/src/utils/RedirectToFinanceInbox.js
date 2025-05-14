import React from "react";
import { Loader } from "@nudmcdgnpm/digit-ui-react-components";

const RedirectToFinanceInbox = () => {

  const baseUrl = process.env.REACT_APP_PROXY_API;
  const redirectPath = "/employee/services/EGF/inbox";

  if (baseUrl && typeof window !== "undefined" && window?.location) {
    window.location.href = `${baseUrl}${redirectPath}`;
  }

  return (
    <div className="loader-container">
      <Loader />
    </div>
  );
};

export default RedirectToFinanceInbox;
