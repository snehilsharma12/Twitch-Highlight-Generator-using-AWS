import {
  Alert,
  Button,
  ButtonGroup,
  Grid,
  GridItem,
  AlertIcon,
  AlertTitle,
  AlertDescription,
  ScaleFade,
} from "@chakra-ui/react";
import { useEffect, useState } from "react";
import {} from "react-router";
import {
  BrowserRouter as Router,
  Routes,
  Route,
  Navigate,
} from "react-router-dom";
import ChannelGrid from "./components/ChannelGrid";
import Clips from "./components/Clips";
import NavBar from "./components/NavBar";
import Register from "./components/Register";
import Login from "./components/Login";
import { getIsUserLoggedIn, logOutUser } from "./services/session";
import SubscribeGrid from "./components/SubscribeGrid";
import { SQSQueueConsumerInitate } from "./SQSQueueConsumer";


function App() {

  useEffect(() => {
    SQSQueueConsumerInitate({showAlertMessage});
  }, []);

  const [searchText, setSearchText] = useState("");
  const [showErrorAlert, setShowErrorAlert] = useState(false);
  const [alertMessage, setAlertMessage] = useState("");
  const [alertType, setAlertType] = useState(AlertTypes.SUCCESS);
  const [showErrorAlertDiv, setShowErrorAlertDiv] = useState(false);
  const handleSearch = (text: string) => {
    setSearchText(text);
  };

  // logOutUser();
  //           location.reload();

  const showAlertMessage = (
    message: any,
    alertType: AlertTypes,
    timeoutInMillis: number
  ) => {
    setAlertMessage(message);
    setAlertType(alertType);
    setShowErrorAlertDiv(true);
    setShowErrorAlert(true);
    setTimeout(() => {
      setShowErrorAlert(false);
    }, timeoutInMillis);
    setTimeout(() => {
      setShowErrorAlertDiv(false);
    }, timeoutInMillis + 100);
  };
  
  return (
    <Grid templateAreas={`"nav" "main"`}>
      <Router>
     {getIsUserLoggedIn() && <GridItem area={"nav"}>
        <NavBar onSearch={handleSearch} />
      </GridItem> }
      <GridItem area="main">
        <ScaleFade
          initialScale={0.9}
          in={showErrorAlert}
          className={showErrorAlertDiv ? "mB10" : "dN"}
        >
          <Alert status={alertType}>
            <AlertIcon />
            <AlertTitle>{alertMessage}</AlertTitle>
          </Alert>
        </ScaleFade>


          <Routes>
            <Route
              path="/:channel_name/clips"
              element={
                getIsUserLoggedIn() ? <Clips /> : <Navigate to="/login" />
              }
            />
            <Route
              path="/"
              element={
                getIsUserLoggedIn() ? (
                  <ChannelGrid searchText={searchText} />
                ) : (
                  <Login showAlertMessage={showAlertMessage} />
                )
              }
            />
            <Route
              path="/subcribedChannels"
              element={
                getIsUserLoggedIn() ? (
                  <SubscribeGrid searchText={searchText} />
                ) : (
                  <Login showAlertMessage={showAlertMessage} />
                )
              }
            />
            <Route
              path="/register"
              element={!getIsUserLoggedIn() ? <Register showAlertMessage={showAlertMessage} /> : <Navigate to={"/"} />}
            />
            <Route path="*" element={<Navigate to="/" />} />
          </Routes>

      </GridItem>
      </Router>
    </Grid>
  );
}

export enum AlertTypes {
  ERROR = "error",
  SUCCESS = "success",
  WARNING = "warning",
  INFO = "info",
}

export default App;
