# Twitch Highlights Generator
In collaboration with: Andrews Rajasekhar, Atharva Lele and Ragu.

- Real-time highlight generation via Twitch API by monitoring chat frequencies.
- Utilizes AWS services:
  - EC2 for hosting the app
  - API Gateway for frontend-backend communication
  - S3 for storage
- Uses Amazon RDS database to store Twitch channel details and highlights.
- Amazon Comprehend performs sentiment analysis on live chat data.
- CloudWatch automates highlight generation based on specified conditions.
- React.js frontend for channels and embedded highlights.

