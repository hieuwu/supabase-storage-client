import SwiftUI
import ComposeApp
import RevenueCat


@main
struct iOSApp: App {
    init() {
        InitKoinKt.doInitKoin(appDeclaration: {_  in } )

        // Initialize RevenueCat
        Purchases.logLevel = .debug
        Purchases.configure(withAPIKey: "api_key")
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
