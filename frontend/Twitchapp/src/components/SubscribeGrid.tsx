import { Center, Heading, list, SimpleGrid } from "@chakra-ui/react";
import React, { useEffect, useState } from "react";
import { getSubscribedChannels } from "../services/api-client";
import ChannelCard from "./ChannelCard";
import ChannelCardSkeleton from "./ChannelCardSkeleton";

export interface Channel {
  twitch_id: number;
  channel_name: string;
  id: number;
}

interface Props {
  searchText: string;
}

const ChannelGrid = ({ searchText }: Props) => {
  const [channels, setchannel] = useState<Channel[]>([]);
  const [error, setError] = useState("");
  const [isLoading, setLoading] = useState(true);
  const skeletons = [1, 2, 3, 4, 5, 6];

  useEffect(() => {
    // getSubscribedChannels()
    //   .then((res) => {
    //     setchannel(res);
    //     setLoading(false);
    //   })
    //   .catch((err) => {
    //     setError(err.message);
    //     setLoading(false);
    //   });
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

  if (filteredChannels.length !== 0)
    return (
      <Center>
        <SimpleGrid columns={3} spacing={10} padding="10px">
          {filteredChannels.map((channel) => (
            <ChannelCard key={channel.id} channel={channel} />
          ))}
          {/* <ChannelCard key="1" /> */}
        </SimpleGrid>
      </Center>
    );
  else
    return (
      <Center>
        {isLoading && (
          <SimpleGrid columns={3} spacing={10} padding="10px">
            {skeletons.map((skeleton) => (
              <ChannelCardSkeleton key={skeleton} />
            ))}
          </SimpleGrid>
        )}
        {!isLoading && <Heading>Subscribed to no Channels</Heading>}
      </Center>
    );
};

export default ChannelGrid;
