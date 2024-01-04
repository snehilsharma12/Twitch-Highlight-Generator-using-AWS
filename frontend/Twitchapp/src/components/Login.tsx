import {
  Box,
  Button,
  Flex,
  FormControl,
  FormLabel,
  Input,
  Link,
  Stack,
} from "@chakra-ui/react";
import React, { useState } from "react";
import { Link as RouteLink, useNavigate } from "react-router-dom";
import { AlertTypes } from "../App";
import '../App.css';
import { logInUser } from "../services/session";


function Login({showAlertMessage}: any){
  const [email, setEmail] = useState("");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [isEmailLogIn, setIsEmailLogIn] = useState(false);
  const navigate = useNavigate();
  
  const loginUser = () => {
    let emailRegex = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
    if(!isEmailLogIn && username.trim() === ""){
      showAlertMessage("Empty Username", AlertTypes.ERROR, 2500);
      return;
    }
    if(password.trim() === ""){
      showAlertMessage("Empty Password", AlertTypes.ERROR, 2500);
      return;
    }
    if(isEmailLogIn && email.trim() === ""){
      showAlertMessage("Empty Email Address", AlertTypes.ERROR, 2500);
      return;
    }

    let isUserLoggedIn: Boolean = false;
    let name = username;
    let isUsername = true;
    if(isEmailLogIn){
      if(!emailRegex.test(email)){
        showAlertMessage("Invalid Email Address", AlertTypes.ERROR, 2500);
      }else{
        name = email;
        isUsername = false;
      }
    }else{
      name = username;
      isUsername = true;
    }
    isUserLoggedIn = logInUser(name, password, isUsername).then((response: any) => {
      if(response.hasOwnProperty("isError") && response.isError){
        if(response.status === 404){
          showAlertMessage("Invalid Credentials", AlertTypes.ERROR, 2500);
        }else if(response.status === 400){
          showAlertMessage("Invalid Data", AlertTypes.ERROR, 2500);
        }else{
          showAlertMessage("Something Went Wrong!", AlertTypes.ERROR, 2500);
        }
      }else{
        navigate("/");
        showAlertMessage("User Logged In", AlertTypes.SUCCESS, 2500);
      }
    }) 
  };

  const setData = (event: any, type: number) => {
    if(type == 0){
      setPassword(event.target.value);
    }else if(type == 1){
      setEmail(event.target.value);
    }else{
      setUsername(event.target.value);
    }
  }

  return (
    <Flex justifyContent={"center"} alignItems="center">
      <Stack justifyContent={"center"} alignItems="center">
        <Box borderColor={"whiteAlpha.600"} borderRadius="20px">
          <form>
            {!isEmailLogIn ? (
            <FormControl>
              <FormLabel htmlFor="username">Username</FormLabel>
              <Input id="username" type={"text"} value={username} onChange={e => setData(e, 2)} placeholder="Username" />
            </FormControl>
            ) : (
              <FormControl>
              <FormLabel htmlFor="email">Email</FormLabel>
              <Input id="email" type={"email"} value={email} onChange={e => setData(e, 1)} placeholder="Email" />
            </FormControl>
            )} 
            <FormControl>
              <FormLabel htmlFor="password">Password</FormLabel>
              <Input id="password" type={"password"} value={password} onChange={e => setData(e, 0)} placeholder="*******" />
            </FormControl>
            <Button className="mB10 mT10 mR10" onClick={loginUser}>Login</Button>
            {isEmailLogIn ? (
            <Button className="mB10 mT10" onClick={() => {setIsEmailLogIn(false)}}>Login Using Username</Button>
          ) : (
          <Button className="mB10 mT10" onClick={() => {setIsEmailLogIn(true)}}>Login Using Email</Button>
          )}
          </form>  
        </Box>
        <Box>
          Don't Have An Account?{" "}
          <Link color={"teal"} as={RouteLink} to={"/register"}>
            Register
          </Link>
        </Box>
      </Stack>
    </Flex>
  );
};

export default Login;
