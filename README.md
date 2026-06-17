<div align="center">
<img width="1080" height="2400" alt="image" src="https://github.com/user-attachments/assets/c519489d-023a-4829-a3e4-328be17b9492" />

</div>

# 一键查询公网IP和地理位置以及运营商细节的网络分析工具，支持网络测速，并附带强大的交互式定制 ICMP 链路延迟探测诊断助手。

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/2d1bf89f-65df-4ce0-8f53-59ba885323eb

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device
