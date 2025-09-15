import React from 'react';
import { View, Text, StyleSheet, Image, ScrollView, TouchableOpacity } from 'react-native';

export function IntroductionSection() {
  return (
    <ScrollView style={styles.container} showsVerticalScrollIndicator={false}>
      {/* Hero Section */}
      <View style={styles.heroSection}>
        <Text style={styles.heroTitle}>Breathe Easier with Better Nutrition</Text>
        <Text style={styles.heroSubtitle}>
          Personalized dietary guidance for managing COPD and improving your quality of life through proper nutrition.
        </Text>
        <TouchableOpacity style={styles.exploreButton}>
          <Text style={styles.exploreButtonText}>Explore Guidelines</Text>
        </TouchableOpacity>
      </View>

      {/* Healthy Food Section */}
      <View style={styles.foodSection}>
        <Text style={styles.sectionTitle}>Healthy food for COPD patients</Text>
      </View>

      {/* Understanding COPD Section */}
      <View style={styles.copdSection}>
        <View style={styles.sectionHeader}>
          <Text style={styles.sectionTitle}>Understanding COPD</Text>
          <Text style={styles.sectionSubtitle}>
            Learn about Chronic Obstructive Pulmonary Disease and its impact on health.
          </Text>
        </View>

        <View style={styles.gridContainer}>
          <View style={styles.card}>
            <Text style={styles.cardTitle}>What is COPD?</Text>
            <Text style={styles.cardText}>
              COPD (Chronic Obstructive Pulmonary Disease) is a chronic lung
              disease that includes conditions such as Emphysema and
              Bronchiectasis. It causes obstructed airflow from the lungs,
              making it difficult to breathe.
            </Text>
            <Text style={styles.cardText}>
              Approximately 16 million adults have been diagnosed with COPD, and
              many more may have the disease without a formal diagnosis. COPD is
              the third leading cause of death globally.
            </Text>
            <View style={styles.imageContainer}>
              <Image
                source={{ uri: 'https://images.unsplash.com/photo-1584118624012-df056829fbd0?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80' }}
                style={styles.image}
                resizeMode="cover"
              />
            </View>
          </View>

          <View style={styles.card}>
            <Text style={styles.cardTitle}>Common Symptoms</Text>
            <View style={styles.symptomsList}>
              {[
                'Shortness of breath, especially during physical activities',
                'Chronic cough that may produce mucus',
                'Wheezing',
                'Chest tightness',
                'Frequent respiratory infections',
                'Lack of energy',
                'Unintended weight loss (in later stages)',
                'Swelling in ankles, feet or legs'
              ].map((symptom, index) => (
                <View key={index} style={styles.symptomItem}>
                  <View style={styles.bullet}>
                    <Text style={styles.bulletText}>•</Text>
                  </View>
                  <Text style={styles.symptomText}>{symptom}</Text>
                </View>
              ))}
            </View>
          </View>
        </View>
      </View>

      {/* Footer Section */}
      <View style={styles.footerSection}>
        <View style={styles.footerHeader}>
          <Text style={styles.footerTitle}>COPD Diet 4 U</Text>
          <Text style={styles.footerSubtitle}>
            Helping you breathe easier with personalized nutritional guidance tailored for COPD management.
          </Text>
        </View>


        <View style={styles.footerLinks}>
          <View style={styles.linkColumn}>
            <Text style={styles.linkTitle}>Recipes</Text>
            <Text style={styles.linkTitle}>Nutrition Guide</Text>
            <Text style={styles.linkTitle}>COPD Management</Text>
            <Text style={styles.linkTitle}>Research</Text>
            <Text style={styles.linkTitle}>Support</Text>
            <Text style={styles.linkTitle}>FAQ</Text>
            <Text style={styles.linkTitle}>Contact</Text>
            <Text style={styles.linkTitle}>Community</Text>
          </View>
          <View style={styles.linkColumn}>
            <Text style={styles.linkTitle}>Company</Text>
            <Text style={styles.linkTitle}>About</Text>
            <Text style={styles.linkTitle}>Our Team</Text>
            <Text style={styles.linkTitle}>Partners</Text>
            <Text style={styles.linkTitle}>Legal</Text>
            <Text style={styles.linkTitle}>Privacy</Text>
            <Text style={styles.linkTitle}>Terms</Text>
          </View>
        </View>

        <View style={styles.copyright}>
          <Text style={styles.copyrightText}>
            © 2023 COPD Diet 4 U. All rights reserved. The information provided is not medical
          </Text>
        </View>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#ffffff',
  },
  heroSection: {
    backgroundColor: '#eff6ff',
    paddingVertical: 48,
    paddingHorizontal: 20,
    alignItems: 'center',
  },
  heroTitle: {
    fontSize: 32,
    fontWeight: '800',
    color: '#1e40af',
    textAlign: 'center',
    marginBottom: 16,
    lineHeight: 40,
  },
  heroSubtitle: {
    fontSize: 18,
    color: '#4b5563',
    textAlign: 'center',
    marginBottom: 32,
    lineHeight: 26,
    maxWidth: 600,
  },
  exploreButton: {
    backgroundColor: '#2563eb',
    paddingHorizontal: 32,
    paddingVertical: 16,
    borderRadius: 8,
  },
  exploreButtonText: {
    color: '#ffffff',
    fontSize: 18,
    fontWeight: '600',
  },
  foodSection: {
    paddingVertical: 32,
    paddingHorizontal: 20,
    backgroundColor: '#f8fafc',
  },
  sectionTitle: {
    fontSize: 24,
    fontWeight: '700',
    color: '#1f2937',
    textAlign: 'center',
  },
  copdSection: {
    paddingVertical: 48,
    paddingHorizontal: 20,
    backgroundColor: '#ffffff',
  },
  sectionHeader: {
    alignItems: 'center',
    marginBottom: 48,
  },
  sectionSubtitle: {
    fontSize: 18,
    color: '#6b7280',
    textAlign: 'center',
    marginTop: 16,
    maxWidth: 600,
    lineHeight: 26,
  },
  gridContainer: {
    gap: 32,
  },
  card: {
    backgroundColor: '#eff6ff',
    borderRadius: 12,
    padding: 24,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  cardTitle: {
    fontSize: 22,
    fontWeight: '600',
    color: '#1e40af',
    marginBottom: 16,
  },
  cardText: {
    fontSize: 16,
    color: '#4b5563',
    lineHeight: 24,
    marginBottom: 16,
  },
  imageContainer: {
    alignItems: 'center',
    marginTop: 24,
  },
  image: {
    width: 280,
    height: 180,
    borderRadius: 8,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  symptomsList: {
    marginTop: 16,
  },
  symptomItem: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    marginBottom: 12,
  },
  bullet: {
    width: 20,
    height: 20,
    borderRadius: 10,
    backgroundColor: '#bfdbfe',
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 8,
    marginTop: 2,
  },
  bulletText: {
    color: '#1d4ed8',
    fontSize: 12,
    fontWeight: 'bold',
  },
  symptomText: {
    flex: 1,
    fontSize: 16,
    color: '#4b5563',
    lineHeight: 22,
  },
  footerSection: {
    backgroundColor: '#1f2937',
    paddingVertical: 48,
    paddingHorizontal: 20,
  },
  footerHeader: {
    alignItems: 'center',
    marginBottom: 32,
  },
  footerTitle: {
    fontSize: 28,
    fontWeight: '700',
    color: '#ffffff',
    marginBottom: 16,
  },
  footerSubtitle: {
    fontSize: 16,
    color: '#d1d5db',
    textAlign: 'center',
    maxWidth: 600,
    lineHeight: 24,
  },
  footerLinks: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    marginBottom: 32,
  },
  linkColumn: {
    alignItems: 'center',
    gap: 12,
  },
  linkTitle: {
    color: '#d1d5db',
    fontSize: 14,
    fontWeight: '500',
  },
  copyright: {
    borderTopWidth: 1,
    borderTopColor: '#374151',
    paddingTop: 24,
    alignItems: 'center',
  },
  copyrightText: {
    color: '#9ca3af',
    fontSize: 14,
    textAlign: 'center',
    lineHeight: 20,
  },
});
