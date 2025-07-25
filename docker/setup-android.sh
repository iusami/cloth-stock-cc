#!/bin/bash
# cloth-stock Android 開発環境セットアップスクリプト

set -e  # エラー時にスクリプトを停止

echo "=== cloth-stock Android 開発環境セットアップ開始 ==="

# 色付き出力用の定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# ログ出力関数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Android SDK の状態確認
check_android_sdk() {
    log_info "Android SDK の状態を確認中..."
    
    if [ ! -d "$ANDROID_HOME" ]; then
        log_error "Android SDK が見つかりません: $ANDROID_HOME"
        return 1
    fi
    
    # SDK Manager の動作確認
    if ! command -v sdkmanager &> /dev/null; then
        log_error "sdkmanager が見つかりません"
        return 1
    fi
    
    log_info "Android SDK が正常に設定されています"
    return 0
}

# 追加のAndroid SDK コンポーネントインストール
install_additional_components() {
    log_info "追加のAndroid SDK コンポーネントをインストール中..."
    
    # システムイメージ（エミュレータ用）
    log_info "Android エミュレータ用システムイメージをインストール中..."
    sdkmanager "system-images;android-34;google_apis;x86_64" || log_warn "システムイメージのインストールに失敗しました"
    
    # エミュレータ本体
    log_info "Android エミュレータをインストール中..."
    sdkmanager "emulator" || log_warn "エミュレータのインストールに失敗しました"
    
    # 追加のビルドツール
    log_info "追加のビルドツールをインストール中..."
    sdkmanager "build-tools;33.0.0" || log_warn "ビルドツール33.0.0のインストールに失敗しました"
}

# AVD（Android Virtual Device）の作成
create_avd() {
    log_info "Android Virtual Device (AVD) を作成中..."
    
    AVD_NAME="cloth_stock_test_device"
    
    # 既存のAVDをチェック
    if avdmanager list avd | grep -q "$AVD_NAME"; then
        log_warn "AVD '$AVD_NAME' は既に存在します"
        return 0
    fi
    
    # AVDを作成
    echo "no" | avdmanager create avd \
        -n "$AVD_NAME" \
        -k "system-images;android-34;google_apis;x86_64" \
        -d "Nexus 5X" || {
        log_warn "AVDの作成に失敗しました（システムイメージが利用できない可能性があります）"
        return 1
    }
    
    log_info "AVD '$AVD_NAME' を作成しました"
}

# Gradle Wrapper の確認と設定
setup_gradle() {
    log_info "Gradle 環境を確認中..."
    
    # Gradle バージョン確認
    if command -v gradle &> /dev/null; then
        log_info "Gradle バージョン: $(gradle --version | head -n 1)"
    else
        log_warn "Gradle が直接インストールされていません（Gradle Wrapperを使用してください）"
    fi
    
    # プロジェクトにGradle Wrapperがあるかチェック
    if [ -f "/workspace/gradlew" ]; then
        log_info "Gradle Wrapper が見つかりました"
        chmod +x /workspace/gradlew
    else
        log_warn "プロジェクトにGradle Wrapperが見つかりません"
    fi
}

# 開発環境の情報表示
show_environment_info() {
    log_info "=== 開発環境情報 ==="
    echo "Android SDK: $ANDROID_HOME"
    echo "Java: $(java -version 2>&1 | head -n 1)"
    echo "作業ディレクトリ: $(pwd)"
    
    if command -v sdkmanager &> /dev/null; then
        echo "インストール済みパッケージ:"
        sdkmanager --list_installed | head -10
    fi
}

# メイン実行部分
main() {
    log_info "cloth-stock Android 開発環境のセットアップを開始します"
    
    # Android SDK確認
    if ! check_android_sdk; then
        log_error "Android SDK の確認に失敗しました"
        exit 1
    fi
    
    # 追加コンポーネントインストール
    install_additional_components
    
    # AVD作成
    create_avd
    
    # Gradle設定
    setup_gradle
    
    # 環境情報表示
    show_environment_info
    
    log_info "=== セットアップ完了 ==="
    log_info "次のコマンドでAndroidプロジェクトをビルドできます:"
    log_info "  ./gradlew build"
    log_info "エミュレータを起動するには:"
    log_info "  emulator -avd cloth_stock_test_device"
}

# スクリプト実行
main "$@"