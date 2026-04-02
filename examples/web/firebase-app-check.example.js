import {initializeApp} from "firebase/app";
import {
  initializeAppCheck,
  ReCaptchaEnterpriseProvider,
  CustomProvider,
} from "firebase/app-check";

const firebaseConfig = {
  apiKey: "YOUR_API_KEY",
  authDomain: "YOUR_PROJECT.firebaseapp.com",
  projectId: "YOUR_PROJECT",
  storageBucket: "YOUR_PROJECT.firebasestorage.app",
  appId: "YOUR_WEB_APP_ID",
};

const app = initializeApp(firebaseConfig);

export function installRecaptchaEnterpriseAppCheck() {
  return initializeAppCheck(app, {
    provider: new ReCaptchaEnterpriseProvider("YOUR_RECAPTCHA_ENTERPRISE_SITE_KEY"),
    isTokenAutoRefreshEnabled: true,
  });
}

export function installTurnstileCompatibleAppCheck() {
  return initializeAppCheck(app, {
    provider: new CustomProvider({
      getToken: async () => {
        const turnstileToken = await window.turnstile.execute("YOUR_TURNSTILE_SITE_KEY", {
          action: "firebase_app_check",
        });

        const response = await fetch("/api/app-check-exchange", {
          method: "POST",
          headers: {"Content-Type": "application/json"},
          body: JSON.stringify({turnstileToken}),
          credentials: "include",
        });

        if (!response.ok) {
          throw new Error("App Check Token konnte nicht ausgestellt werden.");
        }

        return response.json();
      },
    }),
    isTokenAutoRefreshEnabled: true,
  });
}
