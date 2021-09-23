#pragma once

#include <atomic>
#include <cassert>
#include <memory>
#include <string>
#include <vector>

#include "rocketmq/RocketMQ.h"

ROCKETMQ_NAMESPACE_BEGIN

class Histogram {
public:
  Histogram(std::string title, int32_t capacity) : title_(std::move(title)), capacity_(capacity) {
    data_.reserve(capacity);
    for (int i = 0; i < capacity_; ++i) {
      data_.push_back(std::unique_ptr<std::atomic<int32_t>>(new std::atomic<int32_t>(0)));
    }
  }

  void countIn(int grade) {
    if (grade < 0) {
      return;
    }

    if (grade >= capacity_) {
      data_[capacity_ - 1]->fetch_add(1, std::memory_order_relaxed);
      return;
    }

    data_[grade]->fetch_add(1, std::memory_order_relaxed);
  }

  /**
   * Change labels of histogram duration the initialization phase only.
   * @return
   */
  std::vector<std::string>& labels() { return labels_; }

  void reportAndReset(std::string& result) {
    assert(labels_.size() == static_cast<std::vector<std::string>::size_type>(capacity_));
    std::vector<int32_t> values;
    values.reserve(capacity_);
    for (auto& item : data_) {
      int value = item->load(std::memory_order_relaxed);
      values.push_back(value);
      item->fetch_sub(value, std::memory_order_relaxed);
    }
    result.clear();
    result.append(title_).append(":");
    for (std::vector<std::string>::size_type i = 0; i < labels_.size(); ++i) {
      if (i) {
        result.append(", ");
      }
      result.append(labels_[i]).append(std::to_string(values[i]));
    }
  }

private:
  std::string title_;
  std::vector<std::unique_ptr<std::atomic<int32_t>>> data_;
  int32_t capacity_;
  std::vector<std::string> labels_;
};

ROCKETMQ_NAMESPACE_END