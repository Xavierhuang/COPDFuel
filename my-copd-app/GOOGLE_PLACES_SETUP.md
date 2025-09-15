# Google Places API Setup Guide

To enable real-time pulmonary rehabilitation program search, you need to set up Google Places API.

## Step 1: Get Google Places API Key

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable the following APIs:
   - Places API
   - Geocoding API
   - Maps JavaScript API
4. Go to "Credentials" and create an API key
5. Restrict the API key to your app's bundle ID for security

## Step 2: Update the API Key

1. Open `/components/ProgramsNearMe.tsx`
2. Find this line:
   ```typescript
   const GOOGLE_PLACES_API_KEY = 'YOUR_GOOGLE_PLACES_API_KEY';
   ```
3. Replace `'YOUR_GOOGLE_PLACES_API_KEY'` with your actual API key

## Step 3: Configure App Permissions

The app will request location permissions. Make sure to:

1. Add location permissions to `app.json`:
   ```json
   {
     "expo": {
       "ios": {
         "infoPlist": {
           "NSLocationWhenInUseUsageDescription": "This app needs location access to find pulmonary rehabilitation programs near you."
         }
       },
       "android": {
         "permissions": ["ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION"]
       }
     }
   }
   ```

## Step 4: Test the Feature

1. Run your app
2. Navigate to COPD Tools → Find Programs Near Me
3. Allow location permissions when prompted
4. The app will search for real pulmonary rehabilitation programs near your location

## Features

- **Automatic Location Detection**: Uses GPS to find programs near the user
- **Manual Location Input**: Users can enter any city/address to search
- **Real-time Data**: Shows actual programs from Google Places
- **Distance Calculation**: Displays accurate distances to each program
- **Contact Integration**: Direct calling and directions to programs
- **Search Filtering**: Filter programs by name, specialty, or location

## Fallback

If the Google Places API is not configured or fails, the app will show sample data to demonstrate the feature.

## Security Note

In production, store your API key securely using environment variables or a secure configuration service. Never commit API keys to version control.
