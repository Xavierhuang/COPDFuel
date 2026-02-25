import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';

export function Footer() {
  return (
    <View style={styles.container}>
      <View style={styles.content}>
        <View style={styles.mainSection}>
          <Text style={styles.title}>COPD Fuel</Text>
          <Text style={styles.description}>
            Helping you breathe easier with personalized nutritional guidance
            tailored for COPD management.
          </Text>
        </View>
        
        <View style={styles.linksSection}>
          <View style={styles.linkColumn}>
            <Text style={styles.columnTitle}>Resources</Text>
            <TouchableOpacity style={styles.link}>
              <Text style={styles.linkText}>Recipes</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.link}>
              <Text style={styles.linkText}>Nutrition Guide</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.link}>
              <Text style={styles.linkText}>COPD Management</Text>
            </TouchableOpacity>
          </View>
          
          <View style={styles.linkColumn}>
            <Text style={styles.columnTitle}>Support</Text>
            <TouchableOpacity style={styles.link}>
              <Text style={styles.linkText}>FAQ</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.link}>
              <Text style={styles.linkText}>Contact</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.link}>
              <Text style={styles.linkText}>Community</Text>
            </TouchableOpacity>
          </View>
        </View>
      </View>
      
      <View style={styles.divider} />
      <View style={styles.copyright}>
        <Text style={styles.copyrightText}>
          &copy; 2024 COPD Fuel. All rights reserved. The information
          provided is not medical advice.
        </Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: '#1e40af',
    paddingVertical: 48,
    paddingBottom: 100, // Extra padding at bottom to prevent overlap
    paddingHorizontal: 16,
  },
  content: {
    maxWidth: 1280,
    alignSelf: 'center',
    width: '100%',
  },
  mainSection: {
    marginBottom: 48,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#ffffff',
    marginBottom: 16,
  },
  description: {
    fontSize: 16,
    color: '#d1d5db',
    lineHeight: 24,
    maxWidth: 400,
  },
  linksSection: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    flexWrap: 'wrap',
  },
  linkColumn: {
    marginBottom: 24,
    minWidth: 120,
  },
  columnTitle: {
    fontSize: 14,
    fontWeight: '600',
    color: '#e5e7eb',
    textTransform: 'uppercase',
    letterSpacing: 0.5,
    marginBottom: 16,
  },
  link: {
    marginBottom: 12,
  },
  linkText: {
    fontSize: 16,
    color: '#d1d5db',
  },
  divider: {
    height: 1,
    backgroundColor: '#374151',
    marginVertical: 32,
  },
  copyright: {
    alignItems: 'center',
  },
  copyrightText: {
    fontSize: 14,
    color: '#9ca3af',
    textAlign: 'center',
    lineHeight: 20,
  },
});
