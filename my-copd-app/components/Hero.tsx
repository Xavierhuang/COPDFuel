import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet, Image, Dimensions } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';

const { width } = Dimensions.get('window');

export function Hero() {
  return (
    <LinearGradient
      colors={['#3b82f6', '#2dd4bf']}
      start={{ x: 0, y: 0 }}
      end={{ x: 1, y: 0 }}
      style={styles.container}
    >
      <View style={styles.content}>
        <View style={styles.textContainer}>
          <Text style={styles.title}>
            Breathe Easier with Better Nutrition
          </Text>
          <Text style={styles.subtitle}>
            Personalized dietary guidance for managing COPD and improving your
            quality of life through proper nutrition.
          </Text>
          <TouchableOpacity style={styles.button}>
            <Text style={styles.buttonText}>Explore Guidelines</Text>
          </TouchableOpacity>
        </View>
        
        <View style={styles.imageContainer}>
          <Image
            source={{ uri: 'https://images.unsplash.com/photo-1505576399279-565b52d4ac71?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80' }}
            style={styles.image}
            resizeMode="cover"
          />
        </View>
      </View>
    </LinearGradient>
  );
}

const styles = StyleSheet.create({
  container: {
    paddingVertical: 64,
    paddingHorizontal: 16,
  },
  content: {
    maxWidth: 1280,
    alignSelf: 'center',
    width: '100%',
  },
  textContainer: {
    flex: 1,
    marginBottom: 40,
  },
  title: {
    fontSize: 36,
    fontWeight: '800',
    color: '#ffffff',
    lineHeight: 40,
    marginBottom: 16,
  },
  subtitle: {
    fontSize: 18,
    color: '#ffffff',
    lineHeight: 24,
    marginBottom: 32,
    maxWidth: 400,
  },
  button: {
    backgroundColor: '#ffffff',
    paddingHorizontal: 24,
    paddingVertical: 12,
    borderRadius: 6,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
    alignSelf: 'flex-start',
  },
  buttonText: {
    color: '#1d4ed8',
    fontSize: 16,
    fontWeight: '500',
  },
  imageContainer: {
    flex: 1,
    alignItems: 'center',
  },
  image: {
    width: width - 64,
    height: 200,
    borderRadius: 8,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.2,
    shadowRadius: 8,
    elevation: 8,
  },
});
