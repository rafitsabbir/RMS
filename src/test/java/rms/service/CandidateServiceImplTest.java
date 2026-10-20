package rms.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** The phone digits the duplicate check compares. */
class CandidateServiceImplTest {

	@Test
	void phoneDigitsIgnoreFormattingAndNeedSixDigits() {
		assertThat(CandidateServiceImpl.phoneDigits("+44 (0)20 7946-0018")).isEqualTo("440207946" + "0018");
		assertThat(CandidateServiceImpl.phoneDigits("000-1001")).isEqualTo("0001001");
		assertThat(CandidateServiceImpl.phoneDigits("12-345")).isNull();
		assertThat(CandidateServiceImpl.phoneDigits("")).isNull();
		assertThat(CandidateServiceImpl.phoneDigits(null)).isNull();
	}
}
