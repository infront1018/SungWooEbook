import React from 'react';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import HomeScreen from '../screens/HomeScreen';
import ViewerScreen from '../screens/ViewerScreen';
import SplitViewerScreen from '../screens/SplitViewerScreen';
import SettingsScreen from '../screens/SettingsScreen';
import { Book } from '../types';
import { Home, Settings as SettingsIcon } from 'lucide-react-native';

export type RootStackParamList = {
  MainTabs: undefined;
  Viewer: { book: Book };
  VideoPlayer: { videoUrl: string };
  DualViewer: { book: Book; videoUrl: string };
};

export type TabParamList = {
  Home: undefined;
  Settings: undefined;
};

const Stack = createNativeStackNavigator<RootStackParamList>();
const Tab = createBottomTabNavigator<TabParamList>();

/**
 * 🏠 하위 탭 내비게이터 (홈 / 설정)
 */
const TabNavigator = () => {
  return (
    <Tab.Navigator
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: '#6366F1',
        tabBarInactiveTintColor: '#94A3B8',
        tabBarStyle: {
          height: 60,
          paddingBottom: 8,
          paddingTop: 8,
          backgroundColor: '#FFFFFF',
          borderTopWidth: 1,
          borderTopColor: '#F1F5F9',
        },
      }}
    >
      <Tab.Screen 
        name="Home" 
        component={HomeScreen} 
        options={{
          tabBarLabel: '책장',
          tabBarIcon: ({ color, size }) => <Home color={color} size={size} {...({} as any)} />,
        }}
      />
      <Tab.Screen 
        name="Settings" 
        component={SettingsScreen} 
        options={{
          tabBarLabel: '설정',
          tabBarIcon: ({ color, size }) => <SettingsIcon color={color} size={size} {...({} as any)} />,
        }}
      />
    </Tab.Navigator>
  );
};

/**
 * 🚀 최상위 루트 내비게이터 (탭 + 공통 뷰어)
 */
export const RootNavigator = () => {
  return (
    <Stack.Navigator 
      initialRouteName="MainTabs"
      screenOptions={{
        headerShown: false,
        animation: 'slide_from_right',
      }}
    >
      {/* 기본 레이어: 하단 탭 */}
      <Stack.Screen name="MainTabs" component={TabNavigator} />
      
      {/* 상위 레이어: 전체화면 뷰어 (탭을 가림) */}
      <Stack.Screen name="Viewer" component={ViewerScreen} />
      <Stack.Screen name="DualViewer" component={SplitViewerScreen} />
      
      {/* 추후 구현 예정 */}
      <Stack.Screen name="VideoPlayer" component={ViewerScreen} /> 
    </Stack.Navigator>
  );
};
