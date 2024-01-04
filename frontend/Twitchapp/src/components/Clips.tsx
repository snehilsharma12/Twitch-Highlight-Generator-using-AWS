import React, { useEffect, useState } from "react";
import { getTwitchAnalysisOfChannel } from "../services/api-client";
import {
  Card,
  CardHeader,
  CardBody,
  Stack,
  Box,
  Heading,
  StackDivider,
  Button,
  HStack,
  AspectRatio,
} from "@chakra-ui/react";
import { AiFillPlaySquare } from "react-icons/ai";
import { useParams, useNavigate } from "react-router-dom";

interface Clip {
  embed_url: string;
}

interface Analysis {
  clip_details: Clip;
  sentimental_analysis: string;
  video_sentimental_analysis: string;
}

interface Clips {
  channel_name: string;
  twitch_analysis: Analysis[];
}

const Clips = () => {
  const [clips, setClips] = useState<Analysis[]>([]);
  const [error, setError] = useState("");
  const [isEmpty, setEmpty] = useState(true);
  const [showVideo, setVideoIndex] = useState(-1);
  const { channel_name } = useParams();
  const [currentClip, setCurrentClip] = useState<Analysis>();
  const [index, setIndex] = useState(0);
  const location = window.location.hostname;

  function showClip(index: any) {
    if (index == showVideo) setVideoIndex(-1);
    setVideoIndex(index);
  }

  function moveToPrevious(index: any) {
    if (index - 1 >= 0) {
      setIndex(index - 1);
      setCurrentClip(clips[index]);
    }
  }

  useEffect(() => {
    getTwitchAnalysisOfChannel(channel_name)
      .then((res) => {
        setClips(res.twitch_analysis);
        if (
          res.twitch_analysis != undefined &&
          res.twitch_analysis.length != 0
        ) {
          setCurrentClip(res.twitch_analysis[0]);
          setEmpty(false);
        }
      })
      .catch((err) => setError(err.message));
  }, []);

  if (!isEmpty)
    return (
      <Card>
        <CardHeader>
          <Heading size="md">Generated Clips</Heading>
        </CardHeader>
        <CardBody>
          <Stack divider={<StackDivider />} spacing="4">
            {clips.map((clip, index) => (
              <Box
                key={index}
                border="1px"
                borderColor="whiteAlpha.500"
                borderRadius={"20px"}
                justifyContent="space-between"
                p="4"
              >
                <HStack justifyContent={"space-between"}>
                  <h4>clip {index + 1}</h4>
                  {clip.video_sentimental_analysis !== null && (
                    <Heading size={"1xl"}>
                      video SA:{clip.video_sentimental_analysis}
                    </Heading>
                  )}
                  <Heading size={"1xl"}>
                    Sentiment Analysis: {clip.sentimental_analysis}
                  </Heading>
                 
                  <Button
                    variant="link"
                    float={"right"}
                    onClick={() => showClip(index)}
                  >
                    <AiFillPlaySquare size={40} />
                  </Button>
                </HStack>
                {showVideo === index && (
                  <AspectRatio>
                    <iframe
                      src={clip?.clip_details.embed_url + "&parent=" + location}
                    ></iframe>
                  </AspectRatio>
                )}
              </Box>
            ))}
          </Stack>
        </CardBody>
      </Card>
    );
  return <Heading size={"2xl"}>No clips generated</Heading>;
};

export default Clips;
