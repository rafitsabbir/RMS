package rms.dao;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import rms.model.OfferInfo;

/** F8: offers are added, read newest first with their maker, and only a pending one changes. */
class OfferDaoImplTest extends MySqlContainerSupport {

	OfferDaoImpl dao;

	@BeforeEach
	void setUp() {
		dao = new OfferDaoImpl();
		dao.setNamedParameterJdbcTemplate(namedParameterJdbcTemplate);
	}

	@Test
	void anOfferIsAddedAndReadWithItsMaker() {
		int key = dao.addOffer(offer("C1", "PENDING"), "U3");

		assertThat(key).isPositive();
		OfferInfo read = dao.findOffer(key);
		assertThat(read.getCandidateid()).isEqualTo("C1");
		assertThat(read.getOfferdate()).isEqualTo(LocalDate.of(2026, 10, 25));
		assertThat(read.getSalaryband()).isEqualTo("Band 3");
		assertThat(read.getStartdate()).isEqualTo(LocalDate.of(2026, 12, 1));
		assertThat(read.isPending()).isTrue();
		assertThat(read.getCreatedby()).isEqualTo("U3");
		assertThat(read.getCreatedbyname()).isNotBlank();
		assertThat(read.getCreatedat()).isNotNull();
		assertThat(dao.findOffer(key + 1)).isNull();
	}

	@Test
	void offersListNewestFirstAndOnlyAPendingOneChanges() {
		int first = dao.addOffer(offer("C1", "DECLINED"), "U3");
		int second = dao.addOffer(offer("C1", "PENDING"), "U3");

		assertThat(dao.getOffers("C1")).extracting(OfferInfo::getOfferkey).containsExactly(second, first);
		assertThat(dao.getOffers("C2")).isEmpty();

		OfferInfo answer = offer("C1", "ACCEPTED");
		answer.setOfferkey(second);
		answer.setRespondeddate(LocalDate.of(2026, 10, 28));
		assertThat(dao.updateOffer(answer, "U1")).isTrue();
		Map<String, Object> row = jdbcTemplate.queryForMap("select * from candidate_offer where offerkey=" + second);
		assertThat(row.get("status")).isEqualTo("ACCEPTED");
		assertThat(row.get("updatedby")).isEqualTo("U1");
		assertThat(row.get("updatedat")).isNotNull();

		// Answered, or another candidate's: not changed
		assertThat(dao.updateOffer(answer, "U1")).isFalse();
		OfferInfo other = offer("C2", "ACCEPTED");
		other.setOfferkey(first);
		assertThat(dao.updateOffer(other, "U1")).isFalse();
	}

	private static OfferInfo offer(String candidateid, String status) {
		OfferInfo offer = new OfferInfo();
		offer.setCandidateid(candidateid);
		offer.setOfferdate(LocalDate.of(2026, 10, 25));
		offer.setSalaryband("Band 3");
		offer.setStartdate(LocalDate.of(2026, 12, 1));
		offer.setStatus(status);
		return offer;
	}
}
