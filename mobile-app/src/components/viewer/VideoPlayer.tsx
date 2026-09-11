import React, { useRef } from 'react';
import { StyleSheet, View, ActivityIndicator } from 'react-native';
import { Video, ResizeMode } from 'expo-av';

interface VideoPlayerProps {
  url: string;
}

/**
 * Expo-av 기반의 HLS 스트리밍 비디오 플레이어.
 * [Android VideoPlayerFragment 대응]
 */
export const VideoPlayer: React.FC<VideoPlayerProps> = ({ url }) => {
  const video = useRef<Video>(null);

  if (!url) {
    return (
      <View style={styles.container}>
        <ActivityIndicator size="large" color="#fff" />
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <Video
        ref={video}
        style={styles.video}
        source={{
          uri: url,
        }}
        useNativeControls
        resizeMode={ResizeMode.CONTAIN}
        isLooping={false}
        shouldPlay
        onError={(error) => console.error('Video Error:', error)}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000',
    justifyContent: 'center',
    alignItems: 'center',
  },
  video: {
    width: '100%',
    height: '100%',
  },
});
