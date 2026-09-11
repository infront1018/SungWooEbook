import { StatusBar } from 'expo-status-bar';
import { StyleSheet, View } from 'react-native';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { BottomSheetModalProvider } from '@gorhom/bottom-sheet';
import { NavigationContainer } from '@react-navigation/native';
import { RootNavigator } from './src/config/navigation';

/**
 * 성우 주니어 이북 - Expo 크로스 플랫폼 클라이언트 (안드로이드/iOS/웹 공용)
 * 2026-04-06: 네이티브 앱 기능 이관 시작 (초기 환경 구축 단계)
 */
export default function App() {
  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <BottomSheetModalProvider>
        <NavigationContainer>
          <RootNavigator />
        </NavigationContainer>
      </BottomSheetModalProvider>
      <StatusBar style="dark" />
    </GestureHandlerRootView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F8FAFC',
  },
});
