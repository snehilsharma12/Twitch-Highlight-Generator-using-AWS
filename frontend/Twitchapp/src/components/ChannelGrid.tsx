import {
  Center,
  Heading,
  HStack,
  list,
  SimpleGrid,
  Switch,
  Text,
} from "@chakra-ui/react";
import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getChannels } from "../services/api-client";
import { getIsUserLoggedIn, logOutUser } from "../services/session";
import ChannelCard from "./ChannelCard";
import ChannelCardSkeleton from "./ChannelCardSkeleton";
import "../App.css";

export interface Channel {
  twitch_id: number;
  channel_name: string;
  id: number;
  is_user_subscribed: boolean;
}

interface Props {
  searchText: string;
}

const ChannelGrid = ({ searchText }: Props) => {
  const [channels, setchannel] = useState<Channel[]>([]);
  const [error, setError] = useState("");
  const [isLoading, setLoading] = useState(true);
  const [showSubscribed, setSubscribed] = useState(true);
  const skeletons = [1, 2, 3, 4, 5, 6];
  const navigate = useNavigate();

  useEffect(() => {
    if (getIsUserLoggedIn()) {
      getChannels()
        .then((res) => {
          setchannel(res);
          setLoading(false);
        })
        .catch((err) => {
          if (
            err.hasOwnProperty("response") &&
            err.response.hasOwnProperty("data") &&
            (err.response.data.status == 401 || err.response.data.status == 400 || err.response.status == 401 || err.response.status == 400)
          ) {
            logOutUser();
            location.reload();
          }
          setError(err.message);
          setLoading(false);
        });
    }
  }, [searchText]);

  const filteredChannels = channels.filter((channel) => {
    if (searchText) {
      return (
        channel.channel_name &&
        channel.channel_name.toLowerCase().includes(searchText.toLowerCase())
      );
    } else {
      return true;
    }
  });

  const showChannels = filteredChannels.filter((channel) => {
    if (showSubscribed) {
      return channel.channel_name && channel.is_user_subscribed;
    } else {
      return true;
    }
  });

  if (filteredChannels.length !== 0)
    return (
      <>
        <HStack className="mT10">
          <Text as="b">Show subscribed channels?</Text>
          <Switch
            isChecked={showSubscribed === true}
            onChange={() => {
              setSubscribed(!showSubscribed);
            }}
          />
        </HStack>
        <Center>
          <SimpleGrid columns={3} spacing={10} padding="10px">
            {showChannels.length > 0 &&
              showChannels.map((channel) => (
                <ChannelCard key={channel.id} channel={channel} />
              ))}
            {showChannels.length <= 0 && (
              <span className="noChannelsSubs">No Channels Subscribed</span>
            )}
          </SimpleGrid>
        </Center>
      </>
    );
  else
    return (
      <Center>
        <SimpleGrid columns={3} spacing={10} padding="10px">
          {skeletons.map((skeleton) => (
            <ChannelCardSkeleton key={skeleton} />
          ))}
        </SimpleGrid>
      </Center>
    );
};

export default ChannelGrid;
