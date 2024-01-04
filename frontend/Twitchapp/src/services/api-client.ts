import axios from "axios";
import { getUserId, getIsUserLoggedIn } from "./session";

const IS_AWS_BUILD = import.meta.env.VITE_IS_AWS_BUILD;

let domain = "http://localhost:5000";
if (IS_AWS_BUILD) {
  domain = import.meta.env.VITE_BACKEND_AWS_URL;
}

const getHeaders = (): any => {
  return{
    userId: getUserId(),
    "Access-Control-Allow-Origin": domain  
  }
}

export const getChannels = async () => {
  console.log(domain);
  const response = await axios.get(domain + "/channels", {headers: getHeaders()});
  return response.data;
};

export const getTwitchAnalysisOfChannel = async (channel_name: any) => {
  const response = await axios.get(domain + "/twitch_analysis", {
    params: {
      channel_name: channel_name,
    },
  });
  return response.data;
};

export const logIn = async (name: any, password: any, isUserName: Boolean) => {
  let username, email;
  if (isUserName) {
    username = name;
  } else {
    email = name;
  }
  let responseAPI = await axios
    .post(domain + "/user/authenticate", {
      username,
      email,
      password,
    })
    .then((response) => {
      return response.data;
    })
    .catch((error) => {
      let data = error.response;
      data.isError = true;
      return data;
    });
  return responseAPI;
};

export const register = async (username: any, password: any, email: any) => {
  let responseAPI = await axios
    .post(domain + "/user/register", {
      username,
      email,
      password,
    })
    .then((response) => {
      return response;
    })
    .catch((error) => {
      let data = error.response;
      data.isError = true;
      return data;
    });
  return responseAPI;
};

export const subscribeChannel = async(channel_id: number) => {
  let responseAPI = await axios
  .post(domain + "/user/subscriptions",{}, {
    headers: getHeaders(),
    params: {
      channel_id: channel_id,
    }
  })
  .then((response) => {
    return response;
  })
  .catch((error) => {
    let data = error.response;
    data.isError = true;
    return data;
  });
return responseAPI;
};

export const unSubscribeChannel = async(channel_id: number) => {
  let responseAPI = await axios
  .delete(domain + "/user/subscriptions", {
    headers: getHeaders(),
    params: {
      channel_id: channel_id,
    }
  })
  .then((response) => {
    return response;
  })
  .catch((error) => {
    let data = error.response;
    data.isError = true;
    return data;
  });
return responseAPI;
};

export const getSubscribedChannels = async () => {};
