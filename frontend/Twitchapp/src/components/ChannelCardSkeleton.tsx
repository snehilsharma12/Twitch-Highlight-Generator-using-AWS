import { Card, CardBody, Skeleton, SkeletonText } from "@chakra-ui/react";
import React from "react";

const ChannelCardSkeleton = () => {
  return (
    <Card borderRadius={10} overflow="hidden" maxW="sm" width="300px">
      <Skeleton height="200px" />
      <CardBody>
        <SkeletonText />
      </CardBody>
    </Card>
  );
};

export default ChannelCardSkeleton;
