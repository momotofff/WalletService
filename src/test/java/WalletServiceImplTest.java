import com.example.wallet.controller.dto.WalletRequest;
import com.example.wallet.controller.dto.WalletResponse;
import com.example.wallet.exception.InsufficientFundsException;
import com.example.wallet.exception.WalletNotFoundException;
import com.example.wallet.model.OperationType;
import com.example.wallet.model.Wallet;
import com.example.wallet.repository.WalletRepository;
import com.example.wallet.service.impl.WalletServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private WalletServiceImpl walletService;

    private UUID walletId;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        walletId = UUID.randomUUID();
        wallet = new Wallet(walletId, new BigDecimal("1000.00"));
    }

    @Test
    void processTransaction_Deposit_ShouldIncreaseBalance() {
        // Arrange
        WalletRequest request = new WalletRequest(
                walletId,
                OperationType.DEPOSIT,
                new BigDecimal("500.00")
        );

        when(walletRepository.findByIdWithLock(walletId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        WalletResponse response = walletService.processTransaction(request);

        // Assert
        assertNotNull(response);
        assertEquals(walletId, response.getWalletId());
        assertEquals(new BigDecimal("1500.00"), response.getBalance());
        verify(walletRepository).save(wallet);
        verify(walletRepository).findByIdWithLock(walletId);
    }

    @Test
    void processTransaction_Deposit_NewWallet_ShouldCreateWallet() {
        // Arrange
        WalletRequest request = new WalletRequest(
                walletId,
                OperationType.DEPOSIT,
                new BigDecimal("500.00")
        );

        when(walletRepository.findByIdWithLock(walletId)).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> {
            Wallet w = invocation.getArgument(0);
            w.setBalance(new BigDecimal("500.00"));
            return w;
        });

        // Act
        WalletResponse response = walletService.processTransaction(request);

        // Assert
        assertNotNull(response);
        assertEquals(walletId, response.getWalletId());
        assertEquals(new BigDecimal("500.00"), response.getBalance());
        verify(walletRepository, times(2)).save(any(Wallet.class));
    }

    @Test
    void processTransaction_Withdraw_ShouldDecreaseBalance() {
        // Arrange
        WalletRequest request = new WalletRequest(
                walletId,
                OperationType.WITHDRAW,
                new BigDecimal("300.00")
        );

        when(walletRepository.findByIdWithLock(walletId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        WalletResponse response = walletService.processTransaction(request);

        // Assert
        assertNotNull(response);
        assertEquals(walletId, response.getWalletId());
        assertEquals(new BigDecimal("700.00"), response.getBalance());
    }

    @Test
    void processTransaction_Withdraw_InsufficientFunds_ShouldThrowException() {
        // Arrange
        WalletRequest request = new WalletRequest(
                walletId,
                OperationType.WITHDRAW,
                new BigDecimal("1500.00")
        );

        when(walletRepository.findByIdWithLock(walletId)).thenReturn(Optional.of(wallet));

        // Act & Assert
        InsufficientFundsException exception = assertThrows(
                InsufficientFundsException.class,
                () -> walletService.processTransaction(request)
        );

        assertTrue(exception.getMessage().contains("Insufficient funds"));
        verify(walletRepository, never()).save(any());
    }

    @Test
    void processTransaction_Withdraw_NonExistentWallet_ShouldThrowException() {
        // Arrange
        WalletRequest request = new WalletRequest(
                walletId,
                OperationType.WITHDRAW,
                new BigDecimal("100.00")
        );

        when(walletRepository.findByIdWithLock(walletId)).thenReturn(Optional.empty());

        // Act & Assert
        WalletNotFoundException exception = assertThrows(
                WalletNotFoundException.class,
                () -> walletService.processTransaction(request)
        );

        assertTrue(exception.getMessage().contains("Wallet not found"));
    }

    @Test
    void getBalance_ExistingWallet_ShouldReturnBalance() {
        // Arrange
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        // Act
        WalletResponse response = walletService.getBalance(walletId);

        // Assert
        assertNotNull(response);
        assertEquals(walletId, response.getWalletId());
        assertEquals(new BigDecimal("1000.00"), response.getBalance());
    }

    @Test
    void getBalance_NonExistentWallet_ShouldThrowException() {
        // Arrange
        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

        // Act & Assert
        WalletNotFoundException exception = assertThrows(
                WalletNotFoundException.class,
                () -> walletService.getBalance(walletId)
        );

        assertTrue(exception.getMessage().contains("Wallet not found"));
    }

    @Test
    void processTransaction_InvalidAmount_ShouldThrowException() {
        // Note: Валидация происходит на уровне контроллера через @Valid
        // Этот тест проверяет, что сервис обрабатывает null amount
        WalletRequest request = new WalletRequest(
                walletId,
                OperationType.DEPOSIT,
                null
        );

        when(walletRepository.findByIdWithLock(walletId)).thenReturn(Optional.of(wallet));

        assertThrows(NullPointerException.class,
                     () -> walletService.processTransaction(request));
    }
}