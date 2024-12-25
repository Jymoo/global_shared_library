#!/usr/bin/env groovy

/**
 * Jenkins Shared Library for Configuring Environment Variables for ZAP Scans.
 *
 * Usage:
 *   EnvironmentConfig()
 */

def call() {
    echo "Configuring environment variables for ZAP Scanner..."

    // Check and set the TARGET_URL environment variable
    if (!env.TARGET_URL) {
        env.TARGET_URL = 'https://badgeting-web-app.vercel.app/' // Default target URL
        echo "TARGET_URL is not set. Using default: ${env.TARGET_URL}"
    } else {
        echo "TARGET_URL is already set: ${env.TARGET_URL}"
    }
}
