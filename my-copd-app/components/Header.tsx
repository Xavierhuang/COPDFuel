import React from 'react';
import { View, Text, StyleSheet } from 'react-native';

export function Header() {
  return (
    <View style={styles.header}>
      <View style={styles.container}>
        <View style={styles.logoContainer}>
          <Text style={styles.logo}>
            <Text style={styles.copdText}>COPD</Text>
                <Text style={styles.fuelText}> Fuel</Text>
          </Text>
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  header: {
    backgroundColor: '#ffffff',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 3,
    elevation: 3,
    zIndex: 10,
    paddingTop: 20, // Move title down by adding top padding
  },
  container: {
    justifyContent: 'center',
    alignItems: 'center',
    height: 64,
    paddingHorizontal: 16,
    maxWidth: 1280,
    alignSelf: 'center',
    width: '100%',
  },
  logoContainer: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  logo: {
    fontSize: 40,
    fontWeight: 'bold',
  },
  copdText: {
    color: '#f97316', // Orange color
  },
  fuelText: {
    color: '#2563eb', // Blue color
  },
});
