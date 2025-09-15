import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { Home, BookOpen, PieChart, HeartPulse, User, Box } from 'lucide-react-native';

type BottomNavigationProps = {
  activeTab: string;
  setActiveTab: (tab: string) => void;
};

export function BottomNavigation({
  activeTab,
  setActiveTab
}: BottomNavigationProps) {
  const tabs = [
    {
      id: 'home',
      label: 'Home',
      icon: Home
    },
    {
      id: 'guidelines',
      label: 'Guidelines',
      icon: BookOpen
    },
    {
      id: 'tracking',
      label: 'Tracking',
      icon: PieChart
    },
    {
      id: 'recipes',
      label: 'Recipes',
      icon: HeartPulse
    },
    {
      id: 'copd-tools',
      label: 'Resources',
      icon: Box
    },
    {
      id: 'profile',
      label: 'Profile',
      icon: User
    }
  ];

  return (
    <View style={styles.container}>
      <View style={styles.tabsContainer}>
        {tabs.map(tab => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.id;
          
          return (
            <TouchableOpacity
              key={tab.id}
              style={styles.tab}
              onPress={() => setActiveTab(tab.id)}
            >
              <Icon 
                size={24} 
                color={isActive ? '#2563eb' : '#6b7280'} 
              />
              <Text style={[
                styles.tabLabel,
                { color: isActive ? '#2563eb' : '#6b7280' }
              ]}>
                {tab.label}
              </Text>
            </TouchableOpacity>
          );
        })}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    position: 'absolute',
    bottom: 0, // Back to the very bottom
    left: 0,
    right: 0,
    backgroundColor: '#ffffff',
    borderTopWidth: 1,
    borderTopColor: '#e5e7eb',
    zIndex: 50,
    padding: 0, // Remove all padding
    margin: 0, // Remove all margins
    shadowColor: '#000', // Keep shadow for depth
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.1,
    shadowRadius: 8,
    elevation: 8,
  },
  tabsContainer: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    alignItems: 'center',
    height: 50, // Reduced height to move tabs up
    marginTop: 60, // Move tabs up by using negative margin
  },
  tab: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    height: '100%',
    marginTop: -90, // Move individual tab content up
  },
  tabLabel: {
    fontSize: 12,
    marginTop: 4,
    fontWeight: '500',
  },
});
