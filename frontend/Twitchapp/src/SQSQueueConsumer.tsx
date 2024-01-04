import { SQSClient, ReceiveMessageCommand, DeleteMessageCommand } from '@aws-sdk/client-sqs';
import { AlertTypes } from './App';
import { getIsUserLoggedIn, getUserId, getUserIdInNumericalFormat } from './services/session';

let triggerAlertMessage: any;

const client = new SQSClient({
  region: "us-east-1",
  credentials: {
    accessKeyId: "AKIA2ESGRN763DVU6DNV",
    secretAccessKey: "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",
  },
});

const command = new ReceiveMessageCommand({
  QueueUrl: "https://sqs.us-east-1.amazonaws.com/697006387197/TwitchData",
  MaxNumberOfMessages: 10,
  WaitTimeSeconds: 10,
});

function isJsonString(str: any) {
    try {
        JSON.parse(str);
    } catch (e) {
        return false;
    }
    return true;
}

function receiveMessages() {
  client.send(command).then((response) => {
    if (response.Messages) {
      response.Messages.forEach((message) => {

        if(message != undefined && message.Body != undefined && isJsonString(message.Body)){
            console.log(message.Body);
            let data = JSON.parse(message.Body).Message;
            if(data != undefined && isJsonString(data)){
                data = JSON.parse(data);
                if(data.hasOwnProperty("userId") && data.hasOwnProperty("channelName") && data.hasOwnProperty("channelId") ){
                    let userIds = [];
                    userIds = data.userId;
                    if(getUserId() != undefined && getUserId() != false && getIsUserLoggedIn() != undefined && getIsUserLoggedIn() != false ){
                        if(userIds.includes(getUserIdInNumericalFormat())){
                            triggerAlertMessage("A Clip have been generated in Channel '" + data.channelName + "'", AlertTypes.INFO, 5000); 
                        }
                    }  
                }
            }
        }

        // Delete the message from the queue
        deleteMessage(message);
      });
    }
  }).catch((err) => {
    console.error(err);
  });
}

function deleteMessage(message: any) {
  const deleteCommand = new DeleteMessageCommand({
    QueueUrl: "https://sqs.us-east-1.amazonaws.com/697006387197/TwitchData",
    ReceiptHandle: message.ReceiptHandle,
  });

  client.send(deleteCommand).catch((err) => {
    console.error(err);
  });
}

export const SQSQueueConsumerInitate = ({showAlertMessage}: any) => {
    triggerAlertMessage = showAlertMessage;
    setInterval(receiveMessages, 1000);

  return null;
}

