# iOS Parity P3.G (Programs Near Me) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the iOS "Find Programs Near Me" screen match Android `ProgramsNearMeFragment` + `fragment_programs_near_me.xml` + `item_program_card.xml` in copy, layout, states, fallbacks and dialogs, using MapKit/CoreLocation in place of Google Places/Geocoding.

**Architecture:** One file, `Views/ProgramsNearMeView.swift`, fully rewritten. A `@MainActor` `ProgramsLocator` `ObservableObject` owns CoreLocation, `CLGeocoder`, `MKLocalSearch` and all of Android's status strings; the view is a straight transcription of the Android layout. The iOS-only map, free-text query field and always-visible "Sample" button are removed. Toasts use `ToastCenter` (P3.0).

**Tech Stack:** SwiftUI, MapKit (`MKLocalSearch`, `MKMapItem`), CoreLocation, iOS 17.

**Spec:** `docs/superpowers/specs/2026-09-19-ios-parity-p3-design.md` §3 row P3.G. Gap list: `docs/superpowers/specs/2026-09-19-ios-parity-p3/audit-home-guidelines-meals-programs.md` §E1–E13. Android sources: `android/app/src/main/java/com/copdhealthtracker/ui/fragments/ProgramsNearMeFragment.kt`, `res/layout/fragment_programs_near_me.xml`, `res/layout/item_program_card.xml`.

## Global Constraints

- Copy verbatim from Android (spec §1.1). Platform substitutions allowed by spec §1.2: MapKit search instead of Google Places, `CLGeocoder` instead of Google Geocoding, Apple Maps instead of Google Maps (dialog positive button reads "Maps").
- Android colours: `textPrimary` #1f2937, `textSecondary` #4b5563, `textTertiary` #6b7280, `backgroundGray` #f8fafc, `lightYellow` #fef3c7, `lightBlue` #eff6ff, `colorPrimary` #2563eb, `colorPrimaryDark` #1e40af, `colorSuccess` #10b981, `colorAccent` #f59e0b, `colorError` #dc2626, `specialtyText` #1e40af, star #fbbf24, specialty pill background #dbeafe.
- Skip the Google-API-only toast "Address search not available…" (audit E13). Keep the sample data exactly as already in the file (verified identical to Android `samplePrograms`, including the fixed distance strings which iOS must show even without a location).
- Depends on P3.0 (`ToastCenter`). `NSLocationWhenInUseUsageDescription` already exists in `project.pbxproj` (lines 293/332).
- One commit in the `COPDFuel` repo after the build passes. Commands run from the parent repo root; use `/usr/bin/git`.

---

### Task 1: Rewrite `ProgramsNearMeView.swift`

**Files:**
- Modify: `COPDFuel/COPDFuel/Views/ProgramsNearMeView.swift` (whole file)

**Interfaces:**
- Consumes: `ToastCenter.shared.show(_:)`, `Color(hex:)`.
- Produces: `ProgramsNearMeView()` (pushed from Resources › Pulm. Rehab inside a `NavigationStack`).

- [ ] **Step 1: Replace the whole file**

```swift
//
//  ProgramsNearMeView.swift
//  COPDFuel
//
//  Find pulmonary rehabilitation programs near the user's location or a
//  typed address. Layout, copy, states, fallbacks and dialogs mirror
//  Android ProgramsNearMeFragment / fragment_programs_near_me.xml /
//  item_program_card.xml. MapKit + CLGeocoder replace Google Places +
//  Geocoding; Apple Maps replaces Google Maps.
//

import Combine
import SwiftUI
import MapKit
import CoreLocation

// MARK: - Model (ProgramsNearMeFragment.Program)

struct NearbyProgram: Identifiable, Equatable {
    let id: String
    let name: String
    let address: String        // street only, e.g. "1234 Medical Center Dr"
    let city: String
    let state: String
    let zipCode: String
    let phone: String          // "Phone not available" when unknown (Android stores the string)
    let distance: String       // "2.3 miles" (sample) / "850m" / "3.4 km" (live)
    let rating: Double
    let specialties: [String]
    let hours: String

    var fullAddress: String { "\(address), \(city), \(state) \(zipCode)" }
    var hasPhone: Bool { phone != "Phone not available" }

    static func == (lhs: NearbyProgram, rhs: NearbyProgram) -> Bool { lhs.id == rhs.id }
}

private enum SamplePrograms {
    static let entries: [NearbyProgram] = [
        NearbyProgram(id: "1", name: "City General Hospital - Pulmonary Rehab",
                      address: "1234 Medical Center Dr", city: "San Francisco", state: "CA", zipCode: "94102",
                      phone: "(415) 555-0123", distance: "2.3 miles", rating: 4.8,
                      specialties: ["COPD Management", "Exercise Training", "Nutrition Counseling"],
                      hours: "Mon-Fri: 8AM-5PM"),
        NearbyProgram(id: "2", name: "Bay Area Respiratory Center",
                      address: "5678 Health Plaza", city: "San Francisco", state: "CA", zipCode: "94105",
                      phone: "(415) 555-0456", distance: "3.7 miles", rating: 4.6,
                      specialties: ["Pulmonary Rehabilitation", "Breathing Techniques", "Lifestyle Coaching"],
                      hours: "Mon-Thu: 7AM-6PM, Fri: 7AM-4PM"),
        NearbyProgram(id: "3", name: "Golden Gate Pulmonary Institute",
                      address: "9012 Wellness Way", city: "San Francisco", state: "CA", zipCode: "94110",
                      phone: "(415) 555-0789", distance: "5.1 miles", rating: 4.9,
                      specialties: ["Advanced COPD Care", "Exercise Physiology", "Mental Health Support"],
                      hours: "Mon-Fri: 6AM-7PM, Sat: 8AM-2PM"),
        NearbyProgram(id: "4", name: "Community Health Pulmonary Program",
                      address: "3456 Community Blvd", city: "Oakland", state: "CA", zipCode: "94601",
                      phone: "(510) 555-0234", distance: "8.2 miles", rating: 4.4,
                      specialties: ["Community Outreach", "Group Therapy", "Family Education"],
                      hours: "Mon-Fri: 9AM-5PM"),
        NearbyProgram(id: "5", name: "Stanford Pulmonary Rehabilitation",
                      address: "7890 University Ave", city: "Palo Alto", state: "CA", zipCode: "94301",
                      phone: "(650) 555-0567", distance: "12.5 miles", rating: 4.9,
                      specialties: ["Research-Based Care", "Advanced Technology", "Multidisciplinary Team"],
                      hours: "Mon-Fri: 7AM-6PM")
    ]
}

// MARK: - View

struct ProgramsNearMeView: View {
    @StateObject private var locator = ProgramsLocator()
    @State private var manualLocation = ""
    @State private var filterQuery = ""
    @State private var callPrompt: NearbyProgram?
    @State private var directionsPrompt: NearbyProgram?

    /// filterPrograms(): name / city / specialty contains query (case-insensitive).
    private var filteredPrograms: [NearbyProgram] {
        let q = filterQuery.lowercased()
        guard !q.isEmpty else { return locator.allPrograms }
        return locator.allPrograms.filter { p in
            p.name.lowercased().contains(q) ||
            p.city.lowercased().contains(q) ||
            p.specialties.contains { $0.lowercased().contains(q) }
        }
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 8) {
                titleSection
                locationSection
                filterSection
                if locator.isLoading {
                    loadingSection
                }
                resultsCount
                if filteredPrograms.isEmpty && !locator.isLoading {
                    noResultsSection
                } else {
                    VStack(spacing: 16) {
                        ForEach(filteredPrograms) { program in
                            ProgramCard(program: program,
                                        onCall: { call(program) },
                                        onDirections: { directionsPrompt = program })
                        }
                    }
                    .padding(16)
                }
                footer
            }
        }
        .background(Color(hex: "f8fafc"))
        .navigationTitle("Find Programs Near Me")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { locator.checkLocationPermission() }
        .alert("Call Program", isPresented: Binding(
            get: { callPrompt != nil }, set: { if !$0 { callPrompt = nil } }
        ), presenting: callPrompt) { program in
            Button("Call") {
                if let url = URL(string: "tel:\(program.phone.filter { $0.isNumber })") {
                    UIApplication.shared.open(url)
                }
            }
            Button("Cancel", role: .cancel) {}
        } message: { program in
            Text("Would you like to call \(program.name) at \(program.phone)?")
        }
        .alert("Get Directions", isPresented: Binding(
            get: { directionsPrompt != nil }, set: { if !$0 { directionsPrompt = nil } }
        ), presenting: directionsPrompt) { program in
            Button("Maps") { openDirections(to: program) }
            Button("Cancel", role: .cancel) {}
        } message: { program in
            Text("Would you like to get directions to \(program.name)?")
        }
    }

    private func call(_ program: NearbyProgram) {
        if program.hasPhone {
            callPrompt = program
        } else {
            ToastCenter.shared.show("Phone number not available")
        }
    }

    private func openDirections(to program: NearbyProgram) {
        let encoded = program.fullAddress.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
        if let url = URL(string: "https://maps.apple.com/?daddr=\(encoded)") {
            UIApplication.shared.open(url)
        }
    }

    // MARK: Sections (fragment_programs_near_me.xml)

    private var titleSection: some View {
        VStack(spacing: 8) {
            Text("Pulmonary Rehabilitation Programs")
                .font(.system(size: 24, weight: .bold))
                .foregroundColor(Color(hex: "1f2937"))
            Text("Find programs near you to help manage your COPD")
                .font(.system(size: 16))
                .foregroundColor(Color(hex: "4b5563"))
        }
        .multilineTextAlignment(.center)
        .frame(maxWidth: .infinity)
        .padding(20)
        .background(Color.white)
    }

    private var locationSection: some View {
        VStack(spacing: 12) {
            Text("Choose Your Location")
                .font(.system(size: 18, weight: .bold))
                .foregroundColor(Color(hex: "1f2937"))
                .padding(.bottom, 4)

            Button(action: { locator.useCurrentLocationTapped() }) {
                LocationOptionRow(symbol: "location.fill", title: "Use Current Location",
                                  status: locator.locationStatus, showCheck: locator.locationFound)
            }
            .buttonStyle(.plain)

            LocationOptionRow(symbol: "magnifyingglass", title: "Search by Address",
                              status: locator.addressStatus, showCheck: locator.addressFound)

            HStack(spacing: 12) {
                TextField("Enter city, state, or address...", text: $manualLocation)
                    .font(.system(size: 16))
                    .padding(12)
                    .background(Color.white)
                    .cornerRadius(8)
                    .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color(hex: "e5e7eb"), lineWidth: 1))
                Button(action: searchLocationTapped) {
                    Text("Search")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 12)
                        .background(Color(hex: "f59e0b"))
                        .cornerRadius(8)
                }
            }
            .padding(16)
            .background(Color(hex: "fef3c7"))
            .padding(.top, 4)

            HStack(spacing: 8) {
                Image(systemName: "location.fill")
                    .font(.system(size: 16))
                    .foregroundColor(Color(hex: "2563eb"))
                Text(locator.permissionGranted
                     ? "Location permission granted"
                     : "Location permission denied. Use manual search.")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(Color(hex: "1e40af"))
                Spacer()
            }
            .padding(16)
            .background(Color(hex: "eff6ff"))
        }
        .padding(20)
        .background(Color.white)
    }

    private func searchLocationTapped() {
        let location = manualLocation.trimmingCharacters(in: .whitespaces)
        if location.isEmpty {
            ToastCenter.shared.show("Please enter a location")
        } else {
            locator.searchByAddress(location)
        }
    }

    private var filterSection: some View {
        HStack(spacing: 12) {
            Image(systemName: "magnifyingglass")
                .foregroundColor(Color(hex: "6b7280"))
            TextField("Search by name, city, or specialty...", text: $filterQuery)
                .font(.system(size: 16))
        }
        .padding(16)
        .background(Color.white)
        .cornerRadius(8)
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color(hex: "e5e7eb"), lineWidth: 1))
        .padding(20)
        .background(Color.white)
    }

    private var loadingSection: some View {
        VStack(spacing: 12) {
            ProgressView()
                .progressViewStyle(CircularProgressViewStyle(tint: Color(hex: "2563eb")))
                .scaleEffect(1.5)
            Text("Searching for programs near you...")
                .font(.system(size: 16))
                .foregroundColor(Color(hex: "4b5563"))
        }
        .frame(maxWidth: .infinity)
        .padding(40)
        .background(Color.white)
    }

    private var resultsCount: some View {
        Text("\(filteredPrograms.count) program\(filteredPrograms.count != 1 ? "s" : "") found")
            .font(.system(size: 14, weight: .bold))
            .foregroundColor(Color(hex: "4b5563"))
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
    }

    private var noResultsSection: some View {
        VStack(spacing: 8) {
            Text("No programs found.")
                .font(.system(size: 18))
                .foregroundColor(Color(hex: "4b5563"))
            Text("Try adjusting your search terms.")
                .font(.system(size: 16))
                .foregroundColor(Color(hex: "6b7280"))
            Button(action: { locator.showSampleData() }) {
                Text("Show Sample Data")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .background(Color(hex: "dc2626"))
                    .cornerRadius(8)
            }
            .padding(.top, 8)
        }
        .frame(maxWidth: .infinity)
        .padding(40)
    }

    private var footer: some View {
        Text("Don't see a program near you? Contact your healthcare provider for recommendations.")
            .font(.system(size: 14))
            .foregroundColor(Color(hex: "4b5563"))
            .multilineTextAlignment(.center)
            .lineSpacing(4)
            .frame(maxWidth: .infinity)
            .padding(20)
            .padding(.top, 12)
    }
}

/// "Use Current Location" / "Search by Address" option card
/// (location_option_background: #f8fafc, radius 12, 2dp #e5e7eb stroke).
private struct LocationOptionRow: View {
    let symbol: String
    let title: String
    let status: String
    let showCheck: Bool

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: symbol)
                .font(.system(size: 24))
                .foregroundColor(Color(hex: "2563eb"))
                .frame(width: 32, height: 32)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(Color(hex: "1f2937"))
                Text(status)
                    .font(.system(size: 14))
                    .foregroundColor(Color(hex: "4b5563"))
            }
            Spacer()
            if showCheck {
                Image(systemName: "checkmark")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(Color(hex: "10b981"))
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: "f8fafc"))
        .cornerRadius(12)
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(hex: "e5e7eb"), lineWidth: 2))
    }
}

// MARK: - Card (item_program_card.xml)

private struct ProgramCard: View {
    let program: NearbyProgram
    let onCall: () -> Void
    let onDirections: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(program.name)
                .font(.system(size: 20, weight: .bold))
                .foregroundColor(Color(hex: "1f2937"))
                .lineSpacing(4)

            HStack(spacing: 8) {
                HStack(spacing: 2) {
                    ForEach(0..<Int(program.rating.rounded(.down)), id: \.self) { _ in
                        Image(systemName: "star.fill")
                            .font(.system(size: 14))
                            .foregroundColor(Color(hex: "fbbf24"))
                    }
                }
                Text(String(format: "%.1f", program.rating))
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(Color(hex: "1f2937"))
                Text("- \(program.distance)")
                    .font(.system(size: 14))
                    .foregroundColor(Color(hex: "4b5563"))
            }

            infoRow(symbol: "mappin.and.ellipse", text: program.fullAddress)
            infoRow(symbol: "phone.fill", text: program.phone)
            infoRow(symbol: "clock", text: program.hours)

            Text("Specialties:")
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(Color(hex: "1f2937"))
            SpecialtyTags(tags: program.specialties)

            HStack(spacing: 16) {
                Button(action: onCall) {
                    Label("Call", systemImage: "phone.fill")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(Color(hex: "10b981"))
                        .cornerRadius(8)
                }
                Button(action: onDirections) {
                    Label("Directions", systemImage: "arrow.triangle.turn.up.right.diamond.fill")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(Color(hex: "2563eb"))
                        .cornerRadius(8)
                }
            }
            .padding(.top, 8)
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white)
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.08), radius: 4, x: 0, y: 2)
    }

    private func infoRow(symbol: String, text: String) -> some View {
        HStack(alignment: .top, spacing: 8) {
            Image(systemName: symbol)
                .font(.system(size: 16))
                .foregroundColor(Color(hex: "4b5563"))
                .frame(width: 20)
            Text(text)
                .font(.system(size: 16))
                .foregroundColor(Color(hex: "4b5563"))
                .lineSpacing(4)
        }
    }
}

/// Wrapping pills: #dbeafe background, radius 20, #1e40af 14sp, padding 12/6.
private struct SpecialtyTags: View {
    let tags: [String]

    var body: some View {
        FlowLayout(spacing: 8) {
            ForEach(tags, id: \.self) { tag in
                Text(tag)
                    .font(.system(size: 14))
                    .foregroundColor(Color(hex: "1e40af"))
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(Color(hex: "dbeafe"))
                    .cornerRadius(20)
            }
        }
    }
}

/// Minimal wrapping layout (Android FlexboxLayout flexWrap="wrap").
private struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let maxWidth = proposal.width ?? .infinity
        var x: CGFloat = 0, y: CGFloat = 0, rowHeight: CGFloat = 0
        for sub in subviews {
            let size = sub.sizeThatFits(.unspecified)
            if x + size.width > maxWidth, x > 0 {
                x = 0; y += rowHeight + spacing; rowHeight = 0
            }
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
        return CGSize(width: maxWidth == .infinity ? x : maxWidth, height: y + rowHeight)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX, y = bounds.minY, rowHeight: CGFloat = 0
        for sub in subviews {
            let size = sub.sizeThatFits(.unspecified)
            if x + size.width > bounds.maxX, x > bounds.minX {
                x = bounds.minX; y += rowHeight + spacing; rowHeight = 0
            }
            sub.place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(size))
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
    }
}

// MARK: - Locator (CoreLocation + CLGeocoder + MKLocalSearch)

@MainActor
final class ProgramsLocator: NSObject, ObservableObject, CLLocationManagerDelegate {
    @Published var allPrograms: [NearbyProgram] = []
    @Published var isLoading = false
    @Published var permissionGranted = false
    @Published var locationStatus = "Tap to get your current location"
    @Published var locationFound = false
    @Published var addressStatus = "Enter any city, state, or address"
    @Published var addressFound = false

    private let manager = CLLocationManager()
    private var awaitingPermission = false
    private var awaitingLocation = false

    override init() {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyHundredMeters
    }

    private var hasPermission: Bool {
        manager.authorizationStatus == .authorizedWhenInUse || manager.authorizationStatus == .authorizedAlways
    }

    // MARK: Android entry points

    /// checkLocationPermission(): granted → UI granted + getCurrentLocation;
    /// else UI denied + request permission (result → getCurrentLocation or sample).
    func checkLocationPermission() {
        if hasPermission {
            permissionGranted = true
            getCurrentLocation()
        } else {
            permissionGranted = false
            requestPermission()
        }
    }

    /// current_location_option tap.
    func useCurrentLocationTapped() {
        if hasPermission { getCurrentLocation() } else { requestPermission() }
    }

    private func requestPermission() {
        if manager.authorizationStatus == .notDetermined {
            awaitingPermission = true
            manager.requestWhenInUseAuthorization()
        } else {
            permissionGranted = false
            showSampleData()
        }
    }

    /// getCurrentLocation() (:270-309).
    private func getCurrentLocation() {
        guard hasPermission else { showSampleData(); return }
        isLoading = true
        allPrograms = []
        locationStatus = "Getting your location..."
        locationFound = false
        addressFound = false
        addressStatus = "Enter any city, state, or address"
        awaitingLocation = true
        manager.requestLocation()
    }

    /// searchByAddress() (:311-383) with CLGeocoder.
    func searchByAddress(_ address: String) {
        isLoading = true
        allPrograms = []
        addressStatus = "Searching..."
        addressFound = false
        locationFound = false
        CLGeocoder().geocodeAddressString(address) { [weak self] placemarks, error in
            Task { @MainActor in
                guard let self else { return }
                if let error {
                    self.isLoading = false
                    self.addressStatus = "Enter any city, state, or address"
                    self.addressFound = false
                    ToastCenter.shared.show("Error: \(error.localizedDescription)")
                    self.showSampleData()
                    return
                }
                guard let placemark = placemarks?.first, let location = placemark.location else {
                    self.isLoading = false
                    self.addressStatus = "Enter any city, state, or address"
                    self.addressFound = false
                    ToastCenter.shared.show("Location not found")
                    self.showSampleData()
                    return
                }
                let formatted = [placemark.name, placemark.locality, placemark.administrativeArea, placemark.postalCode]
                    .compactMap { $0 }.joined(separator: ", ")
                self.addressStatus = formatted.isEmpty ? address : formatted
                self.addressFound = true
                self.locationStatus = "Tap to get your current location"
                self.searchNearbyPrograms(around: location)
            }
        }
    }

    /// showSampleData() (:454-460).
    func showSampleData() {
        allPrograms = SamplePrograms.entries
        isLoading = false
    }

    // MARK: Search (searchNearbyPrograms :385-452, via MKLocalSearch)

    private func searchNearbyPrograms(around location: CLLocation) {
        isLoading = true
        allPrograms = []
        let region = MKCoordinateRegion(center: location.coordinate,
                                        latitudinalMeters: 100_000, longitudinalMeters: 100_000)
        Task {
            var items = await localSearch("pulmonary rehabilitation", region: region)
            if items.isEmpty { items = await localSearch("hospital", region: region) }
            let programs = items.enumerated().map { index, item in
                Self.program(from: item, index: index, origin: location)
            }
            if programs.isEmpty {
                showSampleData()
            } else {
                allPrograms = programs
                isLoading = false
            }
        }
    }

    private func localSearch(_ query: String, region: MKCoordinateRegion) async -> [MKMapItem] {
        let request = MKLocalSearch.Request()
        request.naturalLanguageQuery = query
        request.region = region
        request.resultTypes = .pointOfInterest
        return (try? await MKLocalSearch(request: request).start())?.mapItems ?? []
    }

    @available(iOS, deprecated: 26.0, message: "Uses MKMapItem.placemark; revisit when the minimum is iOS 26.")
    private static func program(from item: MKMapItem, index: Int, origin: CLLocation) -> NearbyProgram {
        let pm = item.placemark
        let street = [pm.subThoroughfare, pm.thoroughfare].compactMap { $0 }.joined(separator: " ")
        let itemLocation = pm.location ?? CLLocation(latitude: pm.coordinate.latitude, longitude: pm.coordinate.longitude)
        return NearbyProgram(
            id: "place_\(index)",
            name: item.name ?? "Unknown",
            address: street.isEmpty ? "Unknown" : street,
            city: pm.locality ?? "Unknown",
            state: pm.administrativeArea ?? "Unknown",
            zipCode: pm.postalCode ?? "Unknown",
            phone: item.phoneNumber ?? "Phone not available",
            distance: distanceText(from: origin, to: itemLocation),
            rating: 0.0,
            specialties: ["Hospital", "Healthcare"],
            hours: "Hours not available"
        )
    }

    /// calculateDistance() (:600-615): "###m" under 1 km, else "%.1f km".
    private static func distanceText(from a: CLLocation, to b: CLLocation) -> String {
        let km = a.distance(from: b) / 1000
        return km < 1 ? "\(Int(km * 1000))m" : String(format: "%.1f km", km)
    }

    // MARK: CLLocationManagerDelegate

    nonisolated func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        let status = manager.authorizationStatus
        Task { @MainActor in
            guard awaitingPermission, status != .notDetermined else { return }
            awaitingPermission = false
            if status == .authorizedWhenInUse || status == .authorizedAlways {
                permissionGranted = true
                getCurrentLocation()
            } else {
                permissionGranted = false
                showSampleData()
            }
        }
    }

    nonisolated func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        let location = locations.last
        Task { @MainActor in
            guard awaitingLocation else { return }
            awaitingLocation = false
            if let location {
                locationStatus = "Location found"
                locationFound = true
                searchNearbyPrograms(around: location)
            } else {
                locationStatus = "Could not get location"
                showSampleData()
            }
        }
    }

    nonisolated func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        Task { @MainActor in
            guard awaitingLocation else { return }
            awaitingLocation = false
            locationStatus = "Error getting location"
            showSampleData()
        }
    }
}
```

- [ ] **Step 2: Confirm no other file references the removed API**

Run: `grep -rn "ProgramsLocator\|NearbyProgramCard\|SampleProgramSeed\|cameraPosition" COPDFuel/COPDFuel --include='*.swift' | grep -v "Views/ProgramsNearMeView.swift"`
Expected: no output.

---

### Task 2: Build, copy check, commit

- [ ] **Step 1: Build**

```bash
xcodebuild -project "COPDFuel/COPDFuel.xcodeproj" -scheme COPDFuel -destination 'generic/platform=iOS Simulator' -derivedDataPath "$HOME/Library/Developer/Xcode/DerivedData/COPDFuel-cli" -quiet build 2>&1 | grep -E "error:" | head; echo "exit=${PIPESTATUS[0]}"
```
Expected `exit=0`. If `MKLocalSearch.start()` async is unavailable, replace `localSearch` with the completion-handler form wrapped in `withCheckedContinuation`.

- [ ] **Step 2: Copy check**

```bash
for s in "Pulmonary Rehabilitation Programs" "Find programs near you to help manage your COPD" "Choose Your Location" "Tap to get your current location" "Enter any city, state, or address" "Enter city, state, or address..." "Location permission denied. Use manual search." "Search by name, city, or specialty..." "Searching for programs near you..." "Try adjusting your search terms." "Show Sample Data" "Don't see a program near you? Contact your healthcare provider for recommendations." "Specialties:" "Phone number not available" "Would you like to get directions to"; do
  a=$(grep -rl -F "$s" android/app/src/main/res/layout android/app/src/main/java/com/copdhealthtracker/ui/fragments/ProgramsNearMeFragment.kt | wc -l | tr -d ' ')
  i=$(grep -c -F "$s" COPDFuel/COPDFuel/Views/ProgramsNearMeView.swift)
  echo "$a android / $i ios  <- $s"
done
```
Expected: non-zero on both sides for every row.

- [ ] **Step 3: Commit**

```bash
cd COPDFuel && /usr/bin/git add -A && /usr/bin/git commit -q -m "iOS parity P3.G: Programs Near Me matches Android layout, states, fallbacks and dialogs

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>" && /usr/bin/git log --oneline -1
```

- [ ] **Step 4: Manual smoke (simulator)**

Resources › Pulm. Rehab › "Find Programs Near Me": permission prompt on first open; deny → status band reads "Location permission denied. Use manual search." and five sample cards with "- 2.3 miles" style distances appear; empty manual search → toast "Please enter a location"; typing "Palo Alto" in the filter leaves one card; clearing everything and typing "zzz" shows "No programs found." with the red "Show Sample Data" button; Call on a sample shows the "Call Program" dialog; Directions shows "Get Directions" with "Maps".
