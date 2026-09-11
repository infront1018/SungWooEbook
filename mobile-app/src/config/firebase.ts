import { initializeApp } from 'firebase/app';
import { getFirestore, initializeFirestore } from 'firebase/firestore';
import { getStorage } from 'firebase/storage';
import { getAuth } from 'firebase/auth';

// Firebase 프로젝트 설정 (google-services.json 기반)
const firebaseConfig = {
  apiKey: "AIzaSyDpDJh3BQMwYy2XeqcMbmakRPRb8KoSgIA",
  authDomain: "sungwooebook-60fbd.firebaseapp.com",
  projectId: "sungwooebook-60fbd",
  storageBucket: "sungwooebook-60fbd.firebasestorage.app",
  messagingSenderId: "999482054277",
  appId: "1:999482054277:android:3261a94d46471a68bdbbec"
};

// 앱 초기화
const app = initializeApp(firebaseConfig);

//Firestore 초기화 (네이티브 모드 'sungwoo-db' 사용 대응)
// 기본 DB가 아닌 별도 ID를 사용할 경우 initializeFirestore를 사용합니다.
export const db = initializeFirestore(app, {
  databaseId: 'sungwoo-db'
});

export const storage = getStorage(app);
export const auth = getAuth(app);

export default app;
