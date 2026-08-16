
// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "exportedFirebaseKmp",
    platforms: [.iOS("15.0"),.macOS("10.13"),.tvOS("12.0"),.watchOS("4.0")],
    products: [
        .library(
            name: "exportedFirebaseKmp",
            type: .static,
            targets: ["exportedFirebaseKmp"])
    ],
    dependencies: [
        .package(url: "https://github.com/firebase/firebase-ios-sdk.git", exact: "12.17.0")
    ],
    targets: [
        .target(
            name: "exportedFirebaseKmp",
            dependencies: [
                .product(name: "FirebaseCore", package: "firebase-ios-sdk"),.product(name: "FirebaseCrashlytics", package: "firebase-ios-sdk"),.product(name: "FirebaseAnalytics", package: "firebase-ios-sdk")
            ],
            path: "Sources"
            
            
        )
        
    ]
)
        