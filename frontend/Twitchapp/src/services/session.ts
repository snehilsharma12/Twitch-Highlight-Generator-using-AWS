import { logIn, register } from "./api-client";

export const logInUser = (name: any, password: any, isUserName: Boolean): any =>{
    let responseApi = logIn(name, password, isUserName);
    responseApi.then((response) => {
        if(response.hasOwnProperty("user_id")){
            localStorage.setItem("isLogIn", "true");
            localStorage.setItem("userid", response.user_id);
            localStorage.setItem("useremail", response.email);
            localStorage.setItem("username", response.user_name);
        }else{
            localStorage.setItem("isLogIn", "false");
            localStorage.removeItem("userid");
            localStorage.removeItem("useremail");
            localStorage.removeItem("username");
        }
        return response;
    });
    return responseApi;
}

export const logOutUser = () =>{
    localStorage.setItem("isLogIn", "false");
    localStorage.removeItem("userid");
    localStorage.removeItem("useremail");
    localStorage.removeItem("username");
}

export const registerUser = (name: any, password: any, email: any): any =>{
    const responseApi = register(name, password, email).then((response) => {
        return response;
    })
    return responseApi;
}

export const getIsUserLoggedIn = () => {
    return localStorage.getItem("isLogIn") != null && Boolean(localStorage.getItem("isLogIn") == "true");
}

export const getUserId = () => {
    return localStorage.getItem("userid") != null && localStorage.getItem("userid");
}

export const getUserIdInNumericalFormat = () => {
    return localStorage.getItem("userid") != null && Number.parseInt(""+localStorage.getItem("userid"));
}

export const getUserEmail = () => {
    return localStorage.getItem("useremail") != null && localStorage.getItem("useremail");
}

export const getUserName = () => {
    return localStorage.getItem("username") != null && localStorage.getItem("username");
}