import SwiftUI
import ComposeApp


@main
struct iOSApp: App {
    init() {
        InitKoinKt.doInitKoin(appDeclaration: { _ in })
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
