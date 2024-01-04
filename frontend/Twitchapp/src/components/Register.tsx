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
import { registerUser } from "../services/session";
import '../App.css';

function Register({showAlertMessage}: any){
  const [email, setEmail] = useState("");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const navigate = useNavigate();

  const backToLogin = () => {
    navigate("/login");
  }
  
  const register = () => {
    let emailRegex = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
    if(username.trim() === ""){
      showAlertMessage("Empty Username", AlertTypes.ERROR, 2500);
      return;
    }
    if(password.trim() === ""){
      showAlertMessage("Empty Password", AlertTypes.ERROR, 2500);
      return;
    }
    if(email.trim() === ""){
      showAlertMessage("Empty Email Address", AlertTypes.ERROR, 2500);
      return;
    }
    
    if(!emailRegex.test(email)){
      showAlertMessage("Invalid Email Address", AlertTypes.ERROR, 2500);
    }else{
      registerUser(username, password, email).then((response: any) => {
        if(response.hasOwnProperty("isError") && response.isError){
          if(response.status === 406){
            showAlertMessage("User with Email Address already exist", AlertTypes.ERROR, 2500);
          }else if(response.status === 400){
            showAlertMessage("Invalid Data", AlertTypes.ERROR, 2500);
          }else{
            showAlertMessage("Something Went Wrong!", AlertTypes.ERROR, 2500);
          }
        }else{
          navigate("/login");
          showAlertMessage("User Registered", AlertTypes.SUCCESS, 2500);
        }
      }) 
    }
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
            <FormControl>
              <FormLabel htmlFor="email">Email</FormLabel>
              <Input id="email" type={"email"} value={email} onChange={e => setData(e, 1)} placeholder="Email" />
            </FormControl>
            <FormControl>
              <FormLabel htmlFor="username">Username</FormLabel>
              <Input id="username" type={"text"} value={username} onChange={e => setData(e, 2)} placeholder="Username" />
            </FormControl>
            <FormControl>
              <FormLabel htmlFor="password">Password</FormLabel>
              <Input id="password" type={"password"} value={password} onChange={e => setData(e, 0)} placeholder="*******" />
            </FormControl>
            <Button className="mB10 mT10 mR10" onClick={register}>Register</Button>
            <Button className="mB10 mT10" onClick={backToLogin}>Back to Login</Button>
          </form>
        </Box>
      </Stack>
    </Flex>
  );


}
export default Register;
