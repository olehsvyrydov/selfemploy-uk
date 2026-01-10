package uk.selfemploy.hmrc.oauth.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.selfemploy.hmrc.oauth.dto.OAuthTokens;
import uk.selfemploy.hmrc.oauth.storage.TokenStorageException.StorageError;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for TokenStorageService.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TokenStorageService")
class TokenStorageServiceTest {

    @Mock
    private TokenStorage primaryStorage;

    @Mock
    private TokenStorage fallbackStorage;

    private TokenStorageService service;

    @BeforeEach
    void setup() {
        service = new TokenStorageService(primaryStorage, fallbackStorage);
    }

    @Nested
    @DisplayName("Save Tokens")
    class SaveTokens {

        @Test
        @DisplayName("should save tokens to primary storage")
        void shouldSaveTokensToPrimaryStorage() {
            OAuthTokens tokens = createTestTokens();
            when(primaryStorage.getStorageType()).thenReturn("Primary");

            service.saveTokens(tokens);

            verify(primaryStorage).save(tokens);
            verify(fallbackStorage, never()).save(any());
        }

        @Test
        @DisplayName("should fall back to secondary storage on primary failure")
        void shouldFallBackToSecondaryStorageOnPrimaryFailure() {
            OAuthTokens tokens = createTestTokens();
            doThrow(new TokenStorageException(StorageError.KEYCHAIN_ACCESS_DENIED))
                .when(primaryStorage).save(any());
            when(primaryStorage.getStorageType()).thenReturn("Primary");
            when(fallbackStorage.getStorageType()).thenReturn("Fallback");

            service.saveTokens(tokens);

            verify(primaryStorage).save(tokens);
            verify(fallbackStorage).save(tokens);
        }

        @Test
        @DisplayName("should throw when both storages fail")
        void shouldThrowWhenBothStoragesFail() {
            OAuthTokens tokens = createTestTokens();
            doThrow(new TokenStorageException(StorageError.KEYCHAIN_ACCESS_DENIED))
                .when(primaryStorage).save(any());
            doThrow(new TokenStorageException(StorageError.FILE_ACCESS_ERROR))
                .when(fallbackStorage).save(any());
            when(primaryStorage.getStorageType()).thenReturn("Primary");
            when(fallbackStorage.getStorageType()).thenReturn("Fallback");

            assertThatThrownBy(() -> service.saveTokens(tokens))
                .isInstanceOf(TokenStorageException.class);
        }
    }

    @Nested
    @DisplayName("Load Tokens")
    class LoadTokens {

        @Test
        @DisplayName("should load tokens from primary storage")
        void shouldLoadTokensFromPrimaryStorage() {
            OAuthTokens tokens = createTestTokens();
            when(primaryStorage.load()).thenReturn(Optional.of(tokens));
            when(primaryStorage.getStorageType()).thenReturn("Primary");

            Optional<OAuthTokens> result = service.loadTokens();

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(tokens);
            verify(fallbackStorage, never()).load();
        }

        @Test
        @DisplayName("should fall back to secondary storage when primary is empty")
        void shouldFallBackToSecondaryStorageWhenPrimaryIsEmpty() {
            OAuthTokens tokens = createTestTokens();
            when(primaryStorage.load()).thenReturn(Optional.empty());
            when(fallbackStorage.load()).thenReturn(Optional.of(tokens));
            when(fallbackStorage.getStorageType()).thenReturn("Fallback");

            Optional<OAuthTokens> result = service.loadTokens();

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(tokens);
        }

        @Test
        @DisplayName("should fall back to secondary storage on primary failure")
        void shouldFallBackToSecondaryStorageOnPrimaryFailure() {
            OAuthTokens tokens = createTestTokens();
            when(primaryStorage.load())
                .thenThrow(new TokenStorageException(StorageError.KEYCHAIN_ACCESS_DENIED));
            when(fallbackStorage.load()).thenReturn(Optional.of(tokens));
            when(primaryStorage.getStorageType()).thenReturn("Primary");
            when(fallbackStorage.getStorageType()).thenReturn("Fallback");

            Optional<OAuthTokens> result = service.loadTokens();

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(tokens);
        }

        @Test
        @DisplayName("should return empty when no tokens in any storage")
        void shouldReturnEmptyWhenNoTokensInAnyStorage() {
            when(primaryStorage.load()).thenReturn(Optional.empty());
            when(fallbackStorage.load()).thenReturn(Optional.empty());

            Optional<OAuthTokens> result = service.loadTokens();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Delete Tokens")
    class DeleteTokens {

        @Test
        @DisplayName("should delete from all storages")
        void shouldDeleteFromAllStorages() {
            service.deleteTokens();

            verify(primaryStorage).delete();
            verify(fallbackStorage).delete();
        }

        @Test
        @DisplayName("should continue deletion even if primary fails")
        void shouldContinueDeletionEvenIfPrimaryFails() {
            doThrow(new TokenStorageException(StorageError.FILE_ACCESS_ERROR))
                .when(primaryStorage).delete();

            service.deleteTokens();

            verify(primaryStorage).delete();
            verify(fallbackStorage).delete();
        }
    }

    @Nested
    @DisplayName("Has Stored Tokens")
    class HasStoredTokens {

        @Test
        @DisplayName("should return true when primary has tokens")
        void shouldReturnTrueWhenPrimaryHasTokens() {
            when(primaryStorage.exists()).thenReturn(true);

            assertThat(service.hasStoredTokens()).isTrue();
        }

        @Test
        @DisplayName("should return true when fallback has tokens")
        void shouldReturnTrueWhenFallbackHasTokens() {
            when(primaryStorage.exists()).thenReturn(false);
            when(fallbackStorage.exists()).thenReturn(true);

            assertThat(service.hasStoredTokens()).isTrue();
        }

        @Test
        @DisplayName("should return false when no tokens stored")
        void shouldReturnFalseWhenNoTokensStored() {
            when(primaryStorage.exists()).thenReturn(false);
            when(fallbackStorage.exists()).thenReturn(false);

            assertThat(service.hasStoredTokens()).isFalse();
        }
    }

    @Nested
    @DisplayName("Storage Type")
    class StorageType {

        @Test
        @DisplayName("should return primary storage type when primary has tokens")
        void shouldReturnPrimaryStorageTypeWhenPrimaryHasTokens() {
            when(primaryStorage.exists()).thenReturn(true);
            when(primaryStorage.getStorageType()).thenReturn("macOS Keychain");

            assertThat(service.getStorageType()).isEqualTo("macOS Keychain");
        }

        @Test
        @DisplayName("should return fallback storage type when only fallback has tokens")
        void shouldReturnFallbackStorageTypeWhenOnlyFallbackHasTokens() {
            when(primaryStorage.exists()).thenReturn(false);
            when(fallbackStorage.exists()).thenReturn(true);
            when(fallbackStorage.getStorageType()).thenReturn("Encrypted File");

            assertThat(service.getStorageType()).isEqualTo("Encrypted File");
        }
    }

    private OAuthTokens createTestTokens() {
        return OAuthTokens.create(
            "access_token_12345",
            "refresh_token_67890",
            14400,
            "Bearer",
            "read:self-assessment"
        );
    }
}
