import SwiftUI
import ComposeApp


@main
struct iOSApp: App {
    init() {
        CrashReportingKt.doInitCrashReporting()
        AnalyticsKt.doInitAnalytics()
        InitKoinKt.doInitKoin(appDeclaration: { _ in })
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
