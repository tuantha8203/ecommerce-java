package com.tuantha8203.shop.customer.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tuantha8203.shop.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, CustomerService.class})
class CustomerRepositoryTest {

    @Autowired
    CustomerRepository customers;

    @Autowired
    CustomerService customerService;

    @Autowired
    EntityManager em;

    @Test
    void timTheoEmailKhongPhanBietHoaThuong() {
        customers.saveAndFlush(new Customer("An@Mail.com", "Nguyễn An", null));

        assertThat(customers.findByEmail("AN@MAIL.COM")).isPresent();
    }

    @Test
    void emailTrungKhacHoaThuongBiDatabaseTuChoi() {
        customers.saveAndFlush(new Customer("an@mail.com", "An", null));

        assertThatThrownBy(() -> customers.saveAndFlush(new Customer("AN@mail.com ", "An 2", null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void diaChiDauTienTuDongLaMacDinh() {
        Customer an = new Customer("an@mail.com", "An", null);
        an.addAddress("An", "0900000001", "1 Lê Lợi", "Phường Bến Thành", "TP Hồ Chí Minh");
        an.addAddress("An", "0900000001", "2 Hai Bà Trưng", "Phường Sài Gòn", "TP Hồ Chí Minh");
        customers.saveAndFlush(an);

        assertThat(an.getAddresses()).filteredOn(Address::isDefault).hasSize(1);
        assertThat(an.defaultAddress()).get().extracting(Address::getStreet).isEqualTo("1 Lê Lợi");
    }

    @Test
    void doiDiaChiMacDinhVanChiCoMotMacDinh() {
        Customer an = new Customer("an@mail.com", "An", null);
        an.addAddress("An", "0900000001", "1 Lê Lợi", "Phường Bến Thành", "TP Hồ Chí Minh");
        Address second = an.addAddress("An", "0900000001", "2 Hai Bà Trưng", "Phường Sài Gòn", "TP Hồ Chí Minh");
        customers.saveAndFlush(an);
        long customerId = an.getId();
        long secondId = second.getId();
        em.clear();

        customerService.changeDefaultAddress(customerId, secondId);
        em.flush();
        em.clear();

        Customer reloaded = customers.findById(customerId).orElseThrow();
        assertThat(reloaded.getAddresses()).filteredOn(Address::isDefault).hasSize(1);
        assertThat(reloaded.defaultAddress()).get().extracting(Address::getId).isEqualTo(secondId);
    }
}
