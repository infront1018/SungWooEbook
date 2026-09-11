import React from 'react';
import { StyleSheet, View, Text, TouchableOpacity, ScrollView, Switch } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { ChevronRight, Bell, Shield, Moon, Info } from 'lucide-react-native';

/**
 * ⚙️ 설정 화면. 
 * 안드로이드의 SettingsFragment를 Expo 맞춤형 프리미엄 디자인으로 이관.
 */
export default function SettingsScreen() {
  const [isDarkMode, setIsDarkMode] = React.useState(false);
  const [pushEnabled, setPushEnabled] = React.useState(true);

  const SettingItem = ({ icon: Icon, title, value, isSwitch, onPress }: any) => (
    <TouchableOpacity 
      style={styles.item} 
      onPress={onPress} 
      disabled={isSwitch}
      activeOpacity={0.7}
    >
      <View style={styles.itemLeft}>
        <View style={styles.iconWrapper}>
          <Icon size={20} color="#475569" strokeWidth={2} {...({} as any)} />
        </View>
        <Text style={styles.itemTitle}>{title}</Text>
      </View>
      {isSwitch ? (
        <Switch 
          value={value} 
          onValueChange={onPress}
          trackColor={{ false: "#CBD5E1", true: "#6366F1" }}
        />
      ) : (
        <ChevronRight size={20} color="#94A3B8" />
      )}
    </TouchableOpacity>
  );

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.headerTitle}>설정</Text>
      </View>
      <ScrollView style={styles.content}>
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>앱 설정</Text>
          <SettingItem 
            icon={Bell} 
            title="푸시 알림" 
            isSwitch 
            value={pushEnabled} 
            onPress={() => setPushEnabled(!pushEnabled)} 
          />
          <SettingItem 
            icon={Moon} 
            title="다크 모드 (베타)" 
            isSwitch 
            value={isDarkMode} 
            onPress={() => setIsDarkMode(!isDarkMode)} 
          />
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>계정 및 보안</Text>
          <SettingItem icon={Shield} title="콘텐츠 캡처 방지 활성됨" />
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>정보</Text>
          <SettingItem icon={Info} title="버전 정보" value="v1.0.0 (Expo Hybrid)" />
          <TouchableOpacity style={styles.termsBtn}>
            <Text style={styles.termsText}>이용약관 및 개인정보 처리방침</Text>
          </TouchableOpacity>
        </View>

        <View style={styles.footer}>
          <Text style={styles.footerText}>© 2026 Sungwoo Junior. All rights reserved.</Text>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F8FAFC',
  },
  header: {
    padding: 24,
    paddingBottom: 12,
  },
  headerTitle: {
    fontSize: 28,
    fontWeight: 'bold',
    color: '#0F172A',
  },
  content: {
    flex: 1,
  },
  section: {
    marginTop: 24,
    paddingHorizontal: 20,
  },
  sectionTitle: {
    fontSize: 13,
    fontWeight: 'bold',
    color: '#94A3B8',
    textTransform: 'uppercase',
    letterSpacing: 1,
    marginBottom: 8,
  },
  item: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 16,
    paddingHorizontal: 16,
    backgroundColor: '#fff',
    borderRadius: 16,
    marginBottom: 8,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.05,
    shadowRadius: 2,
    elevation: 2,
  },
  itemLeft: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  iconWrapper: {
    width: 36,
    height: 36,
    borderRadius: 10,
    backgroundColor: '#F1F5F9',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 12,
  },
  itemTitle: {
    fontSize: 16,
    color: '#334155',
    fontWeight: '500',
  },
  termsBtn: {
    marginTop: 12,
    padding: 12,
  },
  termsText: {
    color: '#6366F1',
    fontSize: 14,
    textAlign: 'center',
    textDecorationLine: 'underline',
  },
  footer: {
    marginTop: 40,
    paddingBottom: 40,
    alignItems: 'center',
  },
  footerText: {
    fontSize: 12,
    color: '#94A3B8',
  }
});
