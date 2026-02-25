import React, { useState, useEffect } from 'react';
import { 
  View, 
  Text, 
  StyleSheet, 
  ScrollView, 
  TouchableOpacity, 
  TextInput,
  Alert,
  Linking,
  ActivityIndicator
} from 'react-native';
import * as Location from 'expo-location';

interface Program {
  id: string;
  name: string;
  address: string;
  city: string;
  state: string;
  zipCode: string;
  phone: string;
  distance: string;
  rating: number;
  specialties: string[];
  hours: string;
  website?: string;
  placeId?: string;
  latitude?: number;
  longitude?: number;
  isOpen?: boolean;
  priceLevel?: number;
  photos?: string[];
}

const samplePrograms: Program[] = [
  {
    id: '1',
    name: 'City General Hospital - Pulmonary Rehab',
    address: '1234 Medical Center Dr',
    city: 'San Francisco',
    state: 'CA',
    zipCode: '94102',
    phone: '(415) 555-0123',
    distance: '2.3 miles',
    rating: 4.8,
    specialties: ['COPD Management', 'Exercise Training', 'Nutrition Counseling'],
    hours: 'Mon-Fri: 8AM-5PM',
    website: 'www.citygeneral.org'
  },
  {
    id: '2',
    name: 'Bay Area Respiratory Center',
    address: '5678 Health Plaza',
    city: 'San Francisco',
    state: 'CA',
    zipCode: '94105',
    phone: '(415) 555-0456',
    distance: '3.7 miles',
    rating: 4.6,
    specialties: ['Pulmonary Rehabilitation', 'Breathing Techniques', 'Lifestyle Coaching'],
    hours: 'Mon-Thu: 7AM-6PM, Fri: 7AM-4PM',
    website: 'www.bayrespiratory.com'
  },
  {
    id: '3',
    name: 'Golden Gate Pulmonary Institute',
    address: '9012 Wellness Way',
    city: 'San Francisco',
    state: 'CA',
    zipCode: '94110',
    phone: '(415) 555-0789',
    distance: '5.1 miles',
    rating: 4.9,
    specialties: ['Advanced COPD Care', 'Exercise Physiology', 'Mental Health Support'],
    hours: 'Mon-Fri: 6AM-7PM, Sat: 8AM-2PM',
    website: 'www.goldengatepulmonary.org'
  },
  {
    id: '4',
    name: 'Community Health Pulmonary Program',
    address: '3456 Community Blvd',
    city: 'Oakland',
    state: 'CA',
    zipCode: '94601',
    phone: '(510) 555-0234',
    distance: '8.2 miles',
    rating: 4.4,
    specialties: ['Community Outreach', 'Group Therapy', 'Family Education'],
    hours: 'Mon-Fri: 9AM-5PM',
    website: 'www.communityhealth.org'
  },
  {
    id: '5',
    name: 'Stanford Pulmonary Rehabilitation',
    address: '7890 University Ave',
    city: 'Palo Alto',
    state: 'CA',
    zipCode: '94301',
    phone: '(650) 555-0567',
    distance: '12.5 miles',
    rating: 4.9,
    specialties: ['Research-Based Care', 'Advanced Technology', 'Multidisciplinary Team'],
    hours: 'Mon-Fri: 7AM-6PM',
    website: 'www.stanfordhealthcare.org'
  }
];

export function ProgramsNearMe() {
  const [searchQuery, setSearchQuery] = useState('');
  const [filteredPrograms, setFilteredPrograms] = useState<Program[]>([]);
  const [userLocation, setUserLocation] = useState<{latitude: number, longitude: number} | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [locationPermission, setLocationPermission] = useState<boolean>(false);
  const [manualLocation, setManualLocation] = useState('');

  // Google Places API Key - In production, this should be stored securely
  const GOOGLE_PLACES_API_KEY = '[REDACTED_GOOGLE_PLACES_KEY]';

  useEffect(() => {
    requestLocationPermission();
  }, []);

  const requestLocationPermission = async () => {
    try {
      const { status } = await Location.requestForegroundPermissionsAsync();
      if (status === 'granted') {
        setLocationPermission(true);
        await getCurrentLocation();
      } else {
        setLocationPermission(false);
        // Fallback to sample data if location permission denied
        setFilteredPrograms(samplePrograms);
        setIsLoading(false);
      }
    } catch (error) {
      console.error('Error requesting location permission:', error);
      setFilteredPrograms(samplePrograms);
      setIsLoading(false);
    }
  };

  const getCurrentLocation = async () => {
    try {
      const location = await Location.getCurrentPositionAsync({});
      setUserLocation({
        latitude: location.coords.latitude,
        longitude: location.coords.longitude
      });
      await searchNearbyPrograms(location.coords.latitude, location.coords.longitude);
    } catch (error) {
      console.error('Error getting location:', error);
      setFilteredPrograms(samplePrograms);
      setIsLoading(false);
    }
  };

  const searchNearbyPrograms = async (lat: number, lng: number) => {
    try {
      setIsLoading(true);
      
      // Search terms for pulmonary rehabilitation programs
      const searchTerms = [
        'pulmonary rehabilitation',
        'respiratory therapy',
        'COPD treatment',
        'lung rehabilitation',
        'breathing therapy',
        'hospital',
        'medical center',
        'healthcare',
        'clinic',
        'rehabilitation center',
        'physical therapy',
        'cardiopulmonary rehabilitation'
      ];

      let allPrograms: Program[] = [];

      for (const term of searchTerms) {
        const programs = await searchGooglePlaces(term, lat, lng);
        allPrograms = [...allPrograms, ...programs];
      }

      // Remove duplicates and filter for relevant programs
      const uniquePrograms = removeDuplicatePrograms(allPrograms);
      const relevantPrograms = filterRelevantPrograms(uniquePrograms);
      
      console.log(`Total programs found: ${allPrograms.length}, after filtering: ${relevantPrograms.length}`);
      
      // If no programs found, show sample data
      if (relevantPrograms.length === 0) {
        console.log('No programs found, showing sample data');
        setFilteredPrograms(samplePrograms);
      } else {
        setFilteredPrograms(relevantPrograms);
      }
    } catch (error) {
      console.error('Error searching programs:', error);
      setFilteredPrograms(samplePrograms);
    } finally {
      setIsLoading(false);
    }
  };

  const searchGooglePlaces = async (query: string, lat: number, lng: number): Promise<Program[]> => {
    try {
      const radius = 50000; // 50km radius
      const url = `https://places.googleapis.com/v1/places:searchNearby?key=${GOOGLE_PLACES_API_KEY}`;
      
      console.log(`Searching for: ${query} at ${lat}, ${lng}`);
      
      const requestBody = {
        includedTypes: ["hospital"],
        maxResultCount: 20,
        locationRestriction: {
          circle: {
            center: {
              latitude: lat,
              longitude: lng
            },
            radius: radius
          }
        }
      };

      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-Goog-FieldMask': 'places.id,places.displayName,places.formattedAddress,places.location,places.rating,places.nationalPhoneNumber,places.regularOpeningHours,places.priceLevel,places.photos'
        },
        body: JSON.stringify(requestBody)
      });
      const data = await response.json();

      console.log(`API Response for "${query}":`, data);

      if (data.places && data.places.length > 0) {
        console.log(`Found ${data.places.length} results for "${query}"`);
        return data.places.map((place: any, index: number) => ({
          id: place.id || `place_${index}`,
          name: place.displayName?.text || 'Unknown Program',
          address: place.formattedAddress || 'Address not available',
          city: extractCityFromAddress(place.formattedAddress),
          state: extractStateFromAddress(place.formattedAddress),
          zipCode: extractZipFromAddress(place.formattedAddress),
          phone: place.nationalPhoneNumber || 'Phone not available',
          distance: calculateDistance(lat, lng, place.location.latitude, place.location.longitude),
          rating: place.rating || 0,
          specialties: [query],
          hours: place.regularOpeningHours ? 'Hours available' : 'Hours not available',
          placeId: place.id,
          latitude: place.location.latitude,
          longitude: place.location.longitude,
          isOpen: place.regularOpeningHours?.openNow,
          priceLevel: place.priceLevel,
          photos: place.photos?.map((photo: any) => photo.name) || []
        }));
      } else {
        console.log(`API Error for "${query}":`, data.error?.message || 'No results found');
        return [];
      }
    } catch (error) {
      console.error('Error fetching from Google Places:', error);
      return [];
    }
  };

  const removeDuplicatePrograms = (programs: Program[]): Program[] => {
    const seen = new Set();
    return programs.filter(program => {
      if (seen.has(program.placeId || program.name)) {
        return false;
      }
      seen.add(program.placeId || program.name);
      return true;
    });
  };

  const filterRelevantPrograms = (programs: Program[]): Program[] => {
    const relevantKeywords = [
      'pulmonary', 'respiratory', 'lung', 'breathing', 'COPD', 'rehabilitation', 
      'therapy', 'hospital', 'medical', 'health', 'clinic', 'center'
    ];
    
    return programs.filter(program => 
      relevantKeywords.some(keyword => 
        program.name.toLowerCase().includes(keyword) ||
        program.specialties.some(specialty => specialty.toLowerCase().includes(keyword))
      )
    );
  };

  const calculateDistance = (lat1: number, lng1: number, lat2: number, lng2: number): string => {
    const R = 6371; // Earth's radius in kilometers
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLng = (lng2 - lng1) * Math.PI / 180;
    const a = Math.sin(dLat/2) * Math.sin(dLat/2) +
              Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
              Math.sin(dLng/2) * Math.sin(dLng/2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    const distance = R * c;
    
    if (distance < 1) {
      return `${Math.round(distance * 1000)}m`;
    } else {
      return `${distance.toFixed(1)}km`;
    }
  };

  const extractCityFromAddress = (address: string): string => {
    if (!address) return 'Unknown';
    const parts = address.split(',');
    return parts[parts.length - 2]?.trim() || 'Unknown';
  };

  const extractStateFromAddress = (address: string): string => {
    if (!address) return 'Unknown';
    const parts = address.split(',');
    const lastPart = parts[parts.length - 1]?.trim();
    return lastPart?.split(' ')[0] || 'Unknown';
  };

  const extractZipFromAddress = (address: string): string => {
    if (!address) return 'Unknown';
    const parts = address.split(',');
    const lastPart = parts[parts.length - 1]?.trim();
    const zipMatch = lastPart?.match(/\d{5}/);
    return zipMatch ? zipMatch[0] : 'Unknown';
  };

  const handleSearch = (query: string) => {
    setSearchQuery(query);
    if (query.trim() === '') {
      // Show all programs (either from Google Places or sample data)
      if (userLocation) {
        searchNearbyPrograms(userLocation.latitude, userLocation.longitude);
      } else {
        setFilteredPrograms(samplePrograms);
      }
    } else {
      const filtered = filteredPrograms.filter(program =>
        program.name.toLowerCase().includes(query.toLowerCase()) ||
        program.city.toLowerCase().includes(query.toLowerCase()) ||
        program.specialties.some(specialty => 
          specialty.toLowerCase().includes(query.toLowerCase())
        )
      );
      setFilteredPrograms(filtered);
    }
  };

  const handleManualLocationSearch = async () => {
    if (!manualLocation.trim()) {
      Alert.alert('Error', 'Please enter a location to search');
      return;
    }

    try {
      setIsLoading(true);
      // Use Google Geocoding API to get coordinates from address
      const geocodeUrl = `https://maps.googleapis.com/maps/api/geocode/json?address=${encodeURIComponent(manualLocation)}&key=${GOOGLE_PLACES_API_KEY}`;
      const response = await fetch(geocodeUrl);
      const data = await response.json();

      if (data.status === 'OK' && data.results.length > 0) {
        const location = data.results[0].geometry.location;
        setUserLocation({ latitude: location.lat, longitude: location.lng });
        await searchNearbyPrograms(location.lat, location.lng);
      } else {
        Alert.alert('Error', 'Could not find the specified location. Please try a different address.');
        setFilteredPrograms(samplePrograms);
      }
    } catch (error) {
      console.error('Error geocoding location:', error);
      Alert.alert('Error', 'Failed to search for the specified location. Using sample data instead.');
      setFilteredPrograms(samplePrograms);
    } finally {
      setIsLoading(false);
    }
  };

  const handleCallProgram = (phone: string, name: string) => {
    if (phone === 'Phone not available') {
      Alert.alert('Phone Not Available', 'Phone number is not available for this program.');
      return;
    }

    Alert.alert(
      'Call Program',
      `Would you like to call ${name} at ${phone}?`,
      [
        { text: 'Cancel', style: 'cancel' },
        { text: 'Call', onPress: () => {
          Linking.openURL(`tel:${phone}`);
        }}
      ]
    );
  };

  const handleGetDirections = (program: Program) => {
    const address = `${program.address}, ${program.city}, ${program.state} ${program.zipCode}`;
    
    Alert.alert(
      'Get Directions',
      `Would you like to get directions to ${program.name}?`,
      [
        { text: 'Cancel', style: 'cancel' },
        { text: 'Apple Maps', onPress: () => {
          Linking.openURL(`http://maps.apple.com/?daddr=${encodeURIComponent(address)}`);
        }},
        { text: 'Google Maps', onPress: () => {
          Linking.openURL(`https://www.google.com/maps/dir/?api=1&destination=${encodeURIComponent(address)}`);
        }}
      ]
    );
  };

  const renderStars = (rating: number) => {
    const stars = [];
    const fullStars = Math.floor(rating);
    const hasHalfStar = rating % 1 !== 0;

    for (let i = 0; i < fullStars; i++) {
      stars.push(<Text key={i} style={styles.star}>⭐</Text>);
    }
    if (hasHalfStar) {
      stars.push(<Text key="half" style={styles.star}>⭐</Text>);
    }
    return stars;
  };

  return (
    <ScrollView style={styles.container} showsVerticalScrollIndicator={false}>
      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.title}>Pulmonary Rehabilitation Programs</Text>
        <Text style={styles.subtitle}>Find programs near you to help manage your COPD</Text>
      </View>

      {/* Location Options */}
      <View style={styles.locationOptionsContainer}>
        <Text style={styles.locationOptionsTitle}>Choose Your Location</Text>
        
        {/* Current Location Option */}
        {locationPermission && (
          <TouchableOpacity 
            style={[styles.locationOption, userLocation && styles.locationOptionActive]}
            onPress={getCurrentLocation}
          >
            <Text style={styles.locationOptionIcon}>📍</Text>
            <View style={styles.locationOptionText}>
              <Text style={styles.locationOptionTitle}>Use Current Location</Text>
              <Text style={styles.locationOptionSubtitle}>
                {userLocation ? 'Location found' : 'Tap to get your current location'}
              </Text>
            </View>
            {userLocation && <Text style={styles.locationOptionCheck}>✓</Text>}
          </TouchableOpacity>
        )}

        {/* Manual Location Option */}
        <View style={[styles.locationOption, manualLocation && styles.locationOptionActive]}>
          <Text style={styles.locationOptionIcon}>🔍</Text>
          <View style={styles.locationOptionText}>
            <Text style={styles.locationOptionTitle}>Search by Address</Text>
            <Text style={styles.locationOptionSubtitle}>Enter any city, state, or address</Text>
          </View>
          {manualLocation && <Text style={styles.locationOptionCheck}>✓</Text>}
        </View>

        {/* Manual Location Input */}
        <View style={styles.manualLocationContainer}>
          <View style={styles.manualLocationInputContainer}>
            <TextInput
              style={styles.manualLocationInput}
              placeholder="Enter city, state, or address..."
              value={manualLocation}
              onChangeText={setManualLocation}
              placeholderTextColor="#9ca3af"
            />
            <TouchableOpacity 
              style={[styles.searchLocationButton, !manualLocation.trim() && styles.searchLocationButtonDisabled]}
              onPress={handleManualLocationSearch}
              disabled={!manualLocation.trim()}
            >
              <Text style={[styles.searchLocationButtonText, !manualLocation.trim() && styles.searchLocationButtonTextDisabled]}>
                Search
              </Text>
            </TouchableOpacity>
          </View>
        </View>

        {/* Location Status */}
        <View style={styles.locationStatusContainer}>
          {locationPermission ? (
            <View style={styles.locationStatus}>
              <Text style={styles.locationIcon}>📍</Text>
              <Text style={styles.locationText}>
                {userLocation ? 'Using your current location' : 'Location permission granted'}
              </Text>
            </View>
          ) : (
            <View style={styles.locationStatus}>
              <Text style={styles.locationIcon}>⚠️</Text>
              <Text style={styles.locationText}>Location permission denied. Use manual search above.</Text>
            </View>
          )}
        </View>
      </View>

      {/* Search Bar */}
      <View style={styles.searchContainer}>
        <TextInput
          style={styles.searchInput}
          placeholder="Search by name, city, or specialty..."
          value={searchQuery}
          onChangeText={handleSearch}
          placeholderTextColor="#9ca3af"
        />
      </View>

      {/* Loading Indicator */}
      {isLoading && (
        <View style={styles.loadingContainer}>
          <ActivityIndicator size="large" color="#3b82f6" />
          <Text style={styles.loadingText}>Searching for programs near you...</Text>
        </View>
      )}

      {/* Debug/Test Button */}
      {!isLoading && filteredPrograms.length === 0 && (
        <View style={styles.debugContainer}>
          <Text style={styles.debugText}>No programs found. This could be due to:</Text>
          <Text style={styles.debugSubtext}>• API key restrictions</Text>
          <Text style={styles.debugSubtext}>• No healthcare facilities in your area</Text>
          <Text style={styles.debugSubtext}>• Network connectivity issues</Text>
          <TouchableOpacity 
            style={styles.sampleDataButton}
            onPress={() => setFilteredPrograms(samplePrograms)}
          >
            <Text style={styles.sampleDataButtonText}>Show Sample Data</Text>
          </TouchableOpacity>
        </View>
      )}

      {/* Results Count */}
      <View style={styles.resultsContainer}>
        <Text style={styles.resultsText}>
          {filteredPrograms.length} program{filteredPrograms.length !== 1 ? 's' : ''} found
        </Text>
      </View>

      {/* Programs List */}
      <View style={styles.programsList}>
        {filteredPrograms.map((program) => (
          <View key={program.id} style={styles.programCard}>
            {/* Program Header */}
            <View style={styles.programHeader}>
              <View style={styles.programInfo}>
                <Text style={styles.programName}>{program.name}</Text>
                <View style={styles.ratingContainer}>
                  <View style={styles.starsContainer}>
                    {renderStars(program.rating)}
                  </View>
                  <Text style={styles.ratingText}>{program.rating}</Text>
                  <Text style={styles.distanceText}>• {program.distance}</Text>
                </View>
              </View>
            </View>

            {/* Address */}
            <View style={styles.addressContainer}>
              <Text style={styles.addressIcon}>📍</Text>
              <Text style={styles.addressText}>
                {program.address}, {program.city}, {program.state} {program.zipCode}
              </Text>
            </View>

            {/* Contact Info */}
            <View style={styles.contactContainer}>
              <Text style={styles.contactIcon}>📞</Text>
              <Text style={styles.contactText}>{program.phone}</Text>
            </View>

            {/* Hours */}
            <View style={styles.hoursContainer}>
              <Text style={styles.hoursIcon}>🕒</Text>
              <Text style={styles.hoursText}>{program.hours}</Text>
            </View>

            {/* Specialties */}
            <View style={styles.specialtiesContainer}>
              <Text style={styles.specialtiesTitle}>Specialties:</Text>
              <View style={styles.specialtiesList}>
                {program.specialties.map((specialty, index) => (
                  <View key={index} style={styles.specialtyTag}>
                    <Text style={styles.specialtyText}>{specialty}</Text>
                  </View>
                ))}
              </View>
            </View>

            {/* Action Buttons */}
            <View style={styles.actionButtons}>
              <TouchableOpacity 
                style={styles.callButton}
                onPress={() => handleCallProgram(program.phone, program.name)}
              >
                <Text style={styles.callButtonText}>📞 Call</Text>
              </TouchableOpacity>
              <TouchableOpacity 
                style={styles.directionsButton}
                onPress={() => handleGetDirections(program)}
              >
                <Text style={styles.directionsButtonText}>🗺️ Directions</Text>
              </TouchableOpacity>
            </View>
          </View>
        ))}
      </View>

      {/* No Results */}
      {filteredPrograms.length === 0 && (
        <View style={styles.noResultsContainer}>
          <Text style={styles.noResultsText}>No programs found matching your search.</Text>
          <Text style={styles.noResultsSubtext}>Try adjusting your search terms.</Text>
        </View>
      )}

      {/* Footer */}
      <View style={styles.footer}>
        <Text style={styles.footerText}>
          Don't see a program near you? Contact your healthcare provider for recommendations.
        </Text>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f8fafc',
  },
  header: {
    padding: 20,
    backgroundColor: '#ffffff',
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
  },
  title: {
    fontSize: 28,
    fontWeight: '800',
    color: '#1f2937',
    marginBottom: 8,
    textAlign: 'center',
  },
  subtitle: {
    fontSize: 16,
    color: '#6b7280',
    textAlign: 'center',
    lineHeight: 22,
  },
  searchContainer: {
    padding: 20,
    backgroundColor: '#ffffff',
  },
  searchInput: {
    backgroundColor: '#f3f4f6',
    borderRadius: 12,
    padding: 16,
    fontSize: 16,
    borderWidth: 1,
    borderColor: '#e5e7eb',
  },
  resultsContainer: {
    paddingHorizontal: 20,
    paddingVertical: 12,
    backgroundColor: '#f8fafc',
  },
  resultsText: {
    fontSize: 14,
    color: '#6b7280',
    fontWeight: '500',
  },
  programsList: {
    padding: 20,
    gap: 16,
  },
  programCard: {
    backgroundColor: '#ffffff',
    borderRadius: 16,
    padding: 20,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 8,
    elevation: 4,
  },
  programHeader: {
    marginBottom: 16,
  },
  programInfo: {
    flex: 1,
  },
  programName: {
    fontSize: 20,
    fontWeight: '700',
    color: '#1f2937',
    marginBottom: 8,
    lineHeight: 26,
  },
  ratingContainer: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  starsContainer: {
    flexDirection: 'row',
    marginRight: 8,
  },
  star: {
    fontSize: 16,
    marginRight: 2,
  },
  ratingText: {
    fontSize: 16,
    fontWeight: '600',
    color: '#1f2937',
    marginRight: 8,
  },
  distanceText: {
    fontSize: 14,
    color: '#6b7280',
  },
  addressContainer: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    marginBottom: 12,
  },
  addressIcon: {
    fontSize: 16,
    marginRight: 8,
    marginTop: 2,
  },
  addressText: {
    flex: 1,
    fontSize: 16,
    color: '#4b5563',
    lineHeight: 22,
  },
  contactContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 12,
  },
  contactIcon: {
    fontSize: 16,
    marginRight: 8,
  },
  contactText: {
    fontSize: 16,
    color: '#4b5563',
  },
  hoursContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 16,
  },
  hoursIcon: {
    fontSize: 16,
    marginRight: 8,
  },
  hoursText: {
    fontSize: 16,
    color: '#4b5563',
  },
  specialtiesContainer: {
    marginBottom: 20,
  },
  specialtiesTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#1f2937',
    marginBottom: 8,
  },
  specialtiesList: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  specialtyTag: {
    backgroundColor: '#dbeafe',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 20,
  },
  specialtyText: {
    fontSize: 14,
    color: '#1e40af',
    fontWeight: '500',
  },
  actionButtons: {
    flexDirection: 'row',
    gap: 12,
  },
  callButton: {
    flex: 1,
    backgroundColor: '#10b981',
    paddingVertical: 12,
    borderRadius: 8,
    alignItems: 'center',
  },
  callButtonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
  },
  directionsButton: {
    flex: 1,
    backgroundColor: '#3b82f6',
    paddingVertical: 12,
    borderRadius: 8,
    alignItems: 'center',
  },
  directionsButtonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
  },
  noResultsContainer: {
    padding: 40,
    alignItems: 'center',
  },
  noResultsText: {
    fontSize: 18,
    color: '#6b7280',
    textAlign: 'center',
    marginBottom: 8,
  },
  noResultsSubtext: {
    fontSize: 16,
    color: '#9ca3af',
    textAlign: 'center',
  },
  footer: {
    padding: 20,
    backgroundColor: '#f3f4f6',
    marginTop: 20,
  },
  footerText: {
    fontSize: 14,
    color: '#6b7280',
    textAlign: 'center',
    lineHeight: 20,
  },
  // Location Features Styles
  locationOptionsContainer: {
    padding: 20,
    backgroundColor: '#ffffff',
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
  },
  locationOptionsTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: '#1f2937',
    marginBottom: 16,
    textAlign: 'center',
  },
  locationOption: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 16,
    backgroundColor: '#f8fafc',
    borderRadius: 12,
    marginBottom: 12,
    borderWidth: 2,
    borderColor: '#e5e7eb',
  },
  locationOptionActive: {
    backgroundColor: '#f0f9ff',
    borderColor: '#3b82f6',
  },
  locationOptionIcon: {
    fontSize: 24,
    marginRight: 12,
  },
  locationOptionText: {
    flex: 1,
  },
  locationOptionTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#1f2937',
    marginBottom: 4,
  },
  locationOptionSubtitle: {
    fontSize: 14,
    color: '#6b7280',
  },
  locationOptionCheck: {
    fontSize: 20,
    color: '#10b981',
    fontWeight: 'bold',
  },
  searchLocationButtonDisabled: {
    backgroundColor: '#d1d5db',
  },
  searchLocationButtonTextDisabled: {
    color: '#9ca3af',
  },
  locationStatusContainer: {
    padding: 16,
    backgroundColor: '#f0f9ff',
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
  },
  locationStatus: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  locationIcon: {
    fontSize: 16,
    marginRight: 8,
  },
  locationText: {
    fontSize: 14,
    color: '#1e40af',
    fontWeight: '500',
  },
  manualLocationContainer: {
    padding: 16,
    backgroundColor: '#fef3c7',
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
  },
  manualLocationTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#92400e',
    marginBottom: 12,
  },
  manualLocationInputContainer: {
    flexDirection: 'row',
    gap: 12,
  },
  manualLocationInput: {
    flex: 1,
    backgroundColor: '#ffffff',
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
    borderWidth: 1,
    borderColor: '#d1d5db',
  },
  searchLocationButton: {
    backgroundColor: '#f59e0b',
    paddingHorizontal: 20,
    paddingVertical: 12,
    borderRadius: 8,
    justifyContent: 'center',
  },
  searchLocationButtonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
  },
  loadingContainer: {
    padding: 40,
    alignItems: 'center',
    backgroundColor: '#ffffff',
  },
  loadingText: {
    fontSize: 16,
    color: '#6b7280',
    marginTop: 12,
    textAlign: 'center',
  },
  debugContainer: {
    padding: 20,
    backgroundColor: '#fef2f2',
    margin: 16,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#fecaca',
  },
  debugText: {
    fontSize: 16,
    color: '#dc2626',
    fontWeight: '600',
    marginBottom: 12,
    textAlign: 'center',
  },
  debugSubtext: {
    fontSize: 14,
    color: '#7f1d1d',
    marginBottom: 8,
    paddingLeft: 8,
  },
  sampleDataButton: {
    backgroundColor: '#dc2626',
    paddingVertical: 12,
    paddingHorizontal: 24,
    borderRadius: 8,
    marginTop: 16,
    alignSelf: 'center',
  },
  sampleDataButtonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
  },
});
