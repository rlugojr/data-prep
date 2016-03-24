package org.talend.dataprep.upgrades;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.talend.dataprep.info.Version;

public class UpgradesTest {

    private Upgrades upgrades;

    @Before
    public void setUp() throws Exception {
        upgrades = new Upgrades();
    }

    @Test
    public void testEmptyUpgrades() throws Exception {
        // when
        final List<UpgradeTask> toTasks = upgrades.getTasks(new Version("1.0.0", "abcd"));
        // then
        assertThat(toTasks, empty());
        // when
        final List<UpgradeTask> fromToTasks = upgrades.getTasks(new Version("0.0.0", "abcd"), new Version("1.0.0", "abcd"));
        // then
        assertThat(fromToTasks, empty());
    }

    @Test
    public void testRegister() throws Exception {
        // given
        final List<UpgradeTask> before = upgrades.getTasks(new Version("1.0.0", "abcd"));
        assertThat(before, empty());

        // when
        upgrades.register(new TestUpgradeTask("0.9.0"));

        // then
        final List<UpgradeTask> after = upgrades.getTasks(new Version("1.0.0", "abcd"));
        assertThat(after.size(), is(1));
    }

    @Test
    public void testVersionMatch() throws Exception {
        // when
        upgrades.register(new TestUpgradeTask("0.9.0"));

        // then
        final List<UpgradeTask> matches = upgrades.getTasks(new Version("0.8.0", "abcd"));
        assertThat(matches.size(), is(0));
    }

    @Test
    public void testVersionMismatch() throws Exception {
        // when
        upgrades.register(new TestUpgradeTask("0.9.0"));

        // then
        final List<UpgradeTask> matches = upgrades.getTasks(new Version("0.9.1", "abcd"));
        assertThat(matches.size(), is(0));
    }


    private static class TestUpgradeTask implements UpgradeTask {

        private final String versionId;

        public TestUpgradeTask(String versionId) {
            this.versionId = versionId;
        }

        @Override
        public Version getVersion() {
            return new Version(versionId, "abcd");
        }

        @Override
        public String getDescription() {
            return "Test migration task";
        }

        @Override
        public String getName() {
            return "Test migration task";
        }

        @Override
        public UpgradeLog call() throws Exception {
            return null;
        }
    }
}