import {
  Button,
  Card,
  CardBody,
  Center,
  Heading,
  HStack,
  Image,
  Link,
} from "@chakra-ui/react";
import React, { useState, useRef } from "react";
import { Channel } from "./ChannelGrid";
import tubbo from "../assets/tubbo.png";
import summit1g from "../assets/summit1g.png";
import shroud from "../assets/shroud.png";
import xQc from "../assets/xQc.png";
import logo from "../assets/Twitch_TV.png";
import { Link as RouteLink } from "react-router-dom";
import { AiFillHeart, AiOutlineHeart } from "react-icons/ai";
import { subscribeChannel, unSubscribeChannel } from "../services/api-client";

const images = [
  {
    src: "https://static-cdn.jtvnw.net/jtv_user_pictures/905af69a-4fd8-42c7-b842-bf4ee4d51b3b-profile_image-300x300.png",
    title: "tubbo",
  },
  {
    src: "https://static-cdn.jtvnw.net/jtv_user_pictures/99aa4739-21d6-40af-86ae-4b4d3457fce4-profile_image-300x300.png",
    title: "summit1g",
  },
  {
    src: "https://static-cdn.jtvnw.net/jtv_user_pictures/7ed5e0c6-0191-4eef-8328-4af6e4ea5318-profile_image-300x300.png",
    title: "shroud",
  },
  {
    src: "https://static-cdn.jtvnw.net/jtv_user_pictures/xqc-profile_image-9298dca608632101-300x300.jpeg",
    title: "xQc",
  },
  {
    src: "https://static-cdn.jtvnw.net/jtv_user_pictures/b3c347ed-1a7a-40a2-8bee-8a7c4426eb33-profile_image-300x300.png",
    title: "nickmercs",
  },
  {
    src: "https://static-cdn.jtvnw.net/jtv_user_pictures/225cee6d-2afc-4e0d-bdeb-ebc863ae9f40-profile_image-300x300.png",
    title: "jerma985",
  },
  {
    src: "https://static-cdn.jtvnw.net/jtv_user_pictures/f04d2a14-8d63-4cd5-a469-7ec2cd6e5ce3-profile_image-300x300.png",
    title: "tarik",
  },
];

function showImage(name: string): string {
  const image = images.find((image) => image.title === name);
  return image ? image.src : logo;
}

const ChannelCard = (channelData: any) => {
  const subRef = useRef(null);
  const [isSubscribed, setSubscribe] = useState(
    channelData.channel.is_user_subscribed
  );
  const [isSubscribeAPIHit, setIsSubscribeAPIHit] = useState(false);
  const [isLive, setIsLive] = useState(false);

  const subscribe = () => {
    if (!isSubscribed) {
      subscribeChannel(channelData.channel.id).then((response) => {
        if (response.hasOwnProperty("isError") && response.isError) {
        } else {
          setSubscribe(!isSubscribed);
          window.location.reload();
        }
      });
    } else {
      unSubscribeChannel(channelData.channel.id).then((response) => {
        if (response.hasOwnProperty("isError") && response.isError) {
        } else {
          setSubscribe(!isSubscribed);
          window.location.reload();
        }
      });
    }
  };
  return (
    <Center>
      <Card borderRadius={10} overflow="hidden" maxW="sm" width="300px">
        {isSubscribed ? (
          <Link as={RouteLink} to={channelData.channel.channel_name + "/clips"}>
            <Image src={showImage(channelData.channel.channel_name)} />
          </Link>
        ) : (
          <Image src={showImage(channelData.channel.channel_name)} />
        )}
        <CardBody>
          <HStack justifyContent={"space-between"}>
            {isSubscribed ? (
              <Link
                as={RouteLink}
                to={channelData.channel.channel_name + "/clips"}
              >
                <Heading fontSize="2xl">
                  {channelData.channel.channel_name}
                </Heading>
              </Link>
            ) : (
              <Heading fontSize="2xl">
                {channelData.channel.channel_name}
              </Heading>
            )}

            {/* {isLive ? (
              <Button
                colorScheme="red"
                size={"xs"}
                borderRadius={"100px"}
                disabled={true}
                variant={"solid"}
              >
                Live
              </Button>
            ) : (
              <Button
                colorScheme="gray"
                size={"xs"}
                borderRadius={"100px"}
                disabled={true}
                variant={"solid"}
              >
                Offline
              </Button>
            )} */}
            {isSubscribed ? (
              <button onClick={subscribe} disabled={isSubscribeAPIHit}>
                <AiFillHeart color="#ff6b81" size={20} />
              </button>
            ) : (
              <button onClick={subscribe} disabled={isSubscribeAPIHit}>
                <AiOutlineHeart size={20} onClick={subscribe} />
              </button>
            )}
          </HStack>
        </CardBody>
      </Card>
    </Center>
  );
};

export default ChannelCard;
