package com.slamur.plagiarism.model;

import java.util.Objects;

public class IdsPair {

        private final String leftId, rightId;

        public IdsPair(String leftId, String rightId) {
            if (leftId.compareTo(rightId) > 0) {
                var tmpId = leftId;
                leftId = rightId;
                rightId = tmpId;
            }

            this.leftId = leftId;
            this.rightId = rightId;
        }

        public String getLeftId() {
            return leftId;
        }

        public String getRightId() {
            return rightId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            IdsPair idsPair = (IdsPair) o;
            return Objects.equals(leftId, idsPair.leftId)
                    && Objects.equals(rightId, idsPair.rightId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(leftId, rightId);
        }
    }