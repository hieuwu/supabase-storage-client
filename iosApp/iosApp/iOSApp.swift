import SwiftUI
import ComposeApp
import RevenueCat


@main
struct iOSApp: App {
    init() {
        InitKoinKt.doInitKoin(appDeclaration: { _ in })

        // Initialize RevenueCat
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
