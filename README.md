# EVCam

EVCam is a multi-camera in-vehicle dashcam app tailored for Geely Galaxy series vehicles, with optional remote monitoring integrations.

## Highlights
- Simultaneous recording and snapshot capture from multiple cameras
- Segmented recording (1 / 3 / 5 minutes)
- Optional timestamp watermark
- Internal storage + USB storage support
- Floating quick-access controls
- Screen-off recording mode
- Remote integrations (DingTalk / Telegram / Feishu)
- Blind-spot related preview and correction tools

## Tech Stack
- Java (Android)
- Camera2 API
- MediaRecorder / MediaCodec
- Material Components
- OkHttp / Glide / WorkManager

## Build
```bash
# Debug
./gradlew assembleDebug

# Release
./gradlew assembleRelease
```

## Quick Notes
- Grant all required permissions before first use.
- If your car model is not directly supported, use custom model configuration.
- This project is provided as-is for learning and testing purposes.
